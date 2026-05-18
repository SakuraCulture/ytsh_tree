package cn.iocoder.yudao.module.ele.dal.redis;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EleOrderLockServiceImpl implements EleOrderLockService {

    @Resource
    private RedissonClient redissonClient;

    public boolean isRedissonAvailable() {
        if (redissonClient == null) {
            return false;
        }
        try {
            redissonClient.getKeys().count();
            return true;
        } catch (RedissonShutdownException e) {
            return false;
        } catch (Exception e) {
            log.warn("【健康检查】Redisson 连接异常: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void lockSync(String platformStoreId, int waitSeconds, int leaseMinutes) {
        RLock lock = getSyncLock(platformStoreId);
        boolean acquired = acquireLock(lock, waitSeconds, leaseMinutes, "门店同步任务", platformStoreId);
        if (!acquired) {
            throw new RuntimeException("门店同步任务正在执行中: " + platformStoreId);
        }
        log.info("【分布式锁】门店{}同步锁获取成功", platformStoreId);
    }

    @Override
    public void lockCompensate(String taskId, int waitSeconds, int leaseMinutes) {
        RLock lock = getCompensateLock(taskId);
        boolean acquired = acquireLock(lock, waitSeconds, leaseMinutes, "补偿任务", taskId);
        if (!acquired) {
            throw new RuntimeException("补偿任务正在执行中: " + taskId);
        }
        log.info("【分布式锁】补偿任务{}锁获取成功", taskId);
    }

    @Override
    public boolean tryLockSync(String platformStoreId, int waitSeconds, int leaseMinutes) {
        RLock lock = getSyncLock(platformStoreId);
        boolean acquired = acquireLock(lock, waitSeconds, leaseMinutes, "门店同步任务", platformStoreId);
        if (acquired) {
            log.info("【分布式锁】门店{}同步锁获取成功", platformStoreId);
        } else {
            log.warn("【分布式锁】门店{}同步锁获取失败，任务正在执行", platformStoreId);
        }
        return acquired;
    }

    @Override
    public boolean tryLockCompensate(String taskId, int waitSeconds, int leaseMinutes) {
        RLock lock = getCompensateLock(taskId);
        boolean acquired = acquireLock(lock, waitSeconds, leaseMinutes, "补偿任务", taskId);
        if (acquired) {
            log.info("【分布式锁】补偿任务{}锁获取成功", taskId);
        } else {
            log.warn("【分布式锁】补偿任务{}锁获取失败，任务正在执行", taskId);
        }
        return acquired;
    }

    @Override
    public void unlockSync(String platformStoreId) {
        RLock lock = getSyncLock(platformStoreId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("【分布式锁】门店{}同步锁释放成功", platformStoreId);
        }
    }

    @Override
    public void unlockCompensate(String taskId) {
        RLock lock = getCompensateLock(taskId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("【分布式锁】补偿任务{}锁释放成功", taskId);
        }
    }

    @Override
    public boolean tryLockOrder(String orderId, int waitSeconds, int leaseSeconds) {
        if (!isRedissonAvailable()) {
            log.error("【分布式锁】Redisson 已关闭，无法获取订单锁，orderId={}", orderId);
            throw new RedissonShutdownException("Redisson is shutdown");
        }
        
        RLock lock = getOrderLock(orderId);
        try {
            boolean acquired = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.info("【分布式锁】订单{}锁获取成功", orderId);
            } else {
                log.warn("【分布式锁】订单{}锁获取失败，订单正在处理中", orderId);
            }
            return acquired;
        } catch (RedissonShutdownException e) {
            log.error("【分布式锁】Redisson 已关闭，无法获取订单锁，orderId={}", orderId);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("【分布式锁】订单{}获取锁被中断", orderId);
            return false;
        }
    }

    @Override
    public void unlockOrder(String orderId) {
        RLock lock = getOrderLock(orderId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("【分布式锁】订单{}锁释放成功", orderId);
        }
    }

    @Override
    public boolean tryLockOrderWithWatchdog(String orderId, int waitSeconds) {
        if (!isRedissonAvailable()) {
            log.error("【分布式锁】Redisson 已关闭，无法获取订单锁，orderId={}", orderId);
            throw new RedissonShutdownException("Redisson is shutdown");
        }

        RLock lock = getOrderLock(orderId);
        try {
            boolean acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.info("【分布式锁】订单{}锁获取成功（看门狗模式）", orderId);
            } else {
                log.warn("【分布式锁】订单{}锁获取失败，订单正在处理中", orderId);
            }
            return acquired;
        } catch (RedissonShutdownException e) {
            log.error("【分布式锁】Redisson 已关闭，无法获取订单锁，orderId={}", orderId);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("【分布式锁】订单{}获取锁被中断", orderId);
            return false;
        }
    }

    @Override
    public boolean tryLockSyncWindow(String platformStoreId, long windowStart, long windowEnd, int waitSeconds, int leaseMinutes) {
        if (!isRedissonAvailable()) {
            log.error("【分布式锁】Redisson 已关闭，无法获取时间窗口锁，platformStoreId={}, window=[{}-{}]",
                    platformStoreId, windowStart, windowEnd);
            throw new RedissonShutdownException("Redisson is shutdown");
        }

        RLock lock = getSyncWindowLock(platformStoreId, windowStart, windowEnd);
        try {
            boolean acquired = lock.tryLock(waitSeconds, leaseMinutes, TimeUnit.MINUTES);
            if (acquired) {
                log.info("【分布式锁】门店{}时间窗口[{}-{}]锁获取成功，租期{}分钟",
                        platformStoreId, windowStart, windowEnd, leaseMinutes);
            } else {
                log.warn("【分布式锁】门店{}时间窗口[{}-{}]锁获取失败，窗口正在同步中",
                        platformStoreId, windowStart, windowEnd);
            }
            return acquired;
        } catch (RedissonShutdownException e) {
            log.error("【分布式锁】Redisson 已关闭，无法获取时间窗口锁，platformStoreId={}, window=[{}-{}]",
                    platformStoreId, windowStart, windowEnd);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("【分布式锁】门店{}时间窗口[{}-{}]获取锁被中断", platformStoreId, windowStart, windowEnd);
            return false;
        }
    }

    @Override
    public void unlockSyncWindow(String platformStoreId, long windowStart, long windowEnd) {
        RLock lock = getSyncWindowLock(platformStoreId, windowStart, windowEnd);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("【分布式锁】门店{}时间窗口[{}-{}]锁释放成功", platformStoreId, windowStart, windowEnd);
        } else {
            log.warn("【分布式锁】门店{}时间窗口[{}-{}]锁未被当前线程持有，跳过释放",
                    platformStoreId, windowStart, windowEnd);
        }
    }

    @Override
    public void lockStoreGoodsFullSyncTask(String lockKey, int waitSeconds, int leaseMinutes) {
        RLock lock = getStoreGoodsFullSyncTaskLock(lockKey);
        boolean acquired = acquireLock(lock, waitSeconds, leaseMinutes, "门店商品全量同步任务", lockKey);
        if (!acquired) {
            throw new RuntimeException("门店商品全量同步任务正在创建中: " + lockKey);
        }
        log.info("【分布式锁】门店商品全量同步任务{}锁获取成功", lockKey);
    }

    @Override
    public void unlockStoreGoodsFullSyncTask(String lockKey) {
        RLock lock = getStoreGoodsFullSyncTaskLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("【分布式锁】门店商品全量同步任务{}锁释放成功", lockKey);
        }
    }

    @Override
    public void lockStoreInventoryBatchTask(String lockKey, int waitSeconds, int leaseMinutes) {
        RLock lock = getStoreInventoryBatchTaskLock(lockKey);
        boolean acquired = acquireLock(lock, waitSeconds, leaseMinutes, "门店库存批量任务", lockKey);
        if (!acquired) {
            throw new RuntimeException("门店库存批量任务正在创建中: " + lockKey);
        }
        log.info("【分布式锁】门店库存批量任务{}锁获取成功", lockKey);
    }

    @Override
    public void unlockStoreInventoryBatchTask(String lockKey) {
        RLock lock = getStoreInventoryBatchTaskLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("【分布式锁】门店库存批量任务{}锁释放成功", lockKey);
        }
    }

    private boolean acquireLock(RLock lock, int waitSeconds, int leaseMinutes, String businessName, String businessId) {
        try {
            return lock.tryLock(waitSeconds, leaseMinutes, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("%s获取锁被中断: %s", businessName, businessId), e);
        }
    }

    private RLock getSyncLock(String platformStoreId) {
        return redissonClient.getLock(
                String.format(EleLockKeyConstants.ORDER_SYNC_LOCK, platformStoreId));
    }

    private RLock getCompensateLock(String taskId) {
        return redissonClient.getLock(
                String.format(EleLockKeyConstants.ORDER_COMPENSATE_LOCK, taskId));
    }

    private RLock getOrderLock(String orderId) {
        return redissonClient.getLock(
                String.format(EleLockKeyConstants.ORDER_ITEM_LOCK, orderId));
    }

    private RLock getSyncWindowLock(String platformStoreId, long windowStart, long windowEnd) {
        return redissonClient.getLock(
                String.format(EleLockKeyConstants.ORDER_SYNC_WINDOW_LOCK, platformStoreId, windowStart, windowEnd));
    }

    private RLock getStoreGoodsFullSyncTaskLock(String lockKey) {
        return redissonClient.getLock(
                String.format(EleLockKeyConstants.STORE_GOODS_FULL_SYNC_TASK_LOCK, lockKey));
    }

    private RLock getStoreInventoryBatchTaskLock(String lockKey) {
        return redissonClient.getLock(
                String.format(EleLockKeyConstants.STORE_INVENTORY_BATCH_TASK_LOCK, lockKey));
    }
}
