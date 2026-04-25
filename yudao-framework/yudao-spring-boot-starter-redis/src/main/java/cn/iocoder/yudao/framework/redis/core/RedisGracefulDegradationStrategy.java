package cn.iocoder.yudao.framework.redis.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisGracefulDegradationStrategy {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private volatile boolean redisAvailable = true;

    private volatile long lastCheckTime = 0;

    private static final long CHECK_INTERVAL_MS = 5000;

    public boolean isRedisAvailable() {
        if (!redisAvailable) {
            long now = System.currentTimeMillis();
            if (now - lastCheckTime > CHECK_INTERVAL_MS) {
                synchronized (this) {
                    if (now - lastCheckTime > CHECK_INTERVAL_MS) {
                        redisAvailable = checkRedisHealth();
                        lastCheckTime = now;
                    }
                }
            }
        }
        return redisAvailable;
    }

    private boolean checkRedisHealth() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            if (!redisAvailable) {
                log.info("Redis 服务已恢复");
            }
            return true;
        } catch (Exception e) {
            if (redisAvailable) {
                log.error("Redis 服务不可用: {}", e.getMessage());
            }
            return false;
        }
    }

    public <T> T executeWithFallback(Callable<T> redisOperation, Callable<T> fallbackOperation) {
        if (!isRedisAvailable()) {
            try {
                log.debug("Redis 不可用，执行降级策略");
                return fallbackOperation.call();
            } catch (Exception e) {
                log.error("降级操作也失败: {}", e.getMessage(), e);
                return null;
            }
        }

        try {
            T result = redisOperation.call();
            redisAvailable = true;
            return result;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 连接失败，执行降级: {}", e.getMessage());
            redisAvailable = false;
            return executeFallback(fallbackOperation);
        } catch (Exception e) {
            if (e.getMessage() != null && 
                (e.getMessage().contains("LOADING") || 
                 e.getMessage().contains("RedisConnectionFailureException"))) {
                log.error("Redis 异常，执行降级: {}", e.getMessage());
                redisAvailable = false;
                return executeFallback(fallbackOperation);
            }
            throw new RuntimeException(e);
        }
    }

    private <T> T executeFallback(Callable<T> fallbackOperation) {
        try {
            return fallbackOperation.call();
        } catch (Exception e) {
            log.error("降级操作失败: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean tryLockWithFallback(String lockKey, long waitTime, long leaseTime, 
                                       Callable<Boolean> lockOperation, 
                                       Callable<Boolean> fallbackOperation) {
        if (!isRedisAvailable()) {
            try {
                log.debug("Redis 不可用，分布式锁降级，直接放行");
                return fallbackOperation.call();
            } catch (Exception e) {
                log.error("降级操作失败: {}", e.getMessage(), e);
                return false;
            }
        }

        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (acquired) {
                try {
                    return lockOperation.call();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            return false;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 分布式锁连接失败，执行降级: {}", e.getMessage());
            redisAvailable = false;
            return executeFallback(fallbackOperation);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("RedisConnectionFailureException")) {
                log.error("Redis 分布式锁异常，执行降级: {}", e.getMessage());
                redisAvailable = false;
                return executeFallback(fallbackOperation);
            }
            throw new RuntimeException(e);
        }
    }

    public void markRedisUnavailable() {
        this.redisAvailable = false;
        this.lastCheckTime = System.currentTimeMillis();
    }

    public void markRedisAvailable() {
        this.redisAvailable = true;
    }
}
