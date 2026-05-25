package cn.iocoder.yudao.module.ele.service.threadpool;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自适应线程池管理器
 *
 * 根据队列使用率和活跃率动态调整线程池大小,避免队列积压和线程满载。
 * 适用于饿了么订单同步等IO密集型任务。
 *
 * @author 优团科技数字化团队
 */
@Component
public class AdaptivePoolManager {

    private static final Logger log = LoggerFactory.getLogger(AdaptivePoolManager.class);

    @Resource
    @Qualifier("eleOrderSyncExecutor")
    private ThreadPoolTaskExecutor syncExecutor;

    @Resource
    @Qualifier("eleStoreGoodsFullSyncExecutor")
    private ThreadPoolTaskExecutor fullSyncExecutor;

    @Resource
    @Qualifier("eleStoreGoodsPageExecutor")
    private ThreadPoolExecutor pageExecutor;

    private static final int MIN_CORE_SIZE = 10;
    private static final int MAX_CORE_SIZE = 50;
    private static final int MAX_POOL_SIZE = 80;
    private static final int QUEUE_HIGH_THRESHOLD = 60;
    private static final int QUEUE_LOW_THRESHOLD = 30;
    private static final int ACTIVE_HIGH_THRESHOLD = 85;
    private static final int ACTIVE_LOW_THRESHOLD = 50;
    private static final int SCALE_UP_STEP = 5;
    private static final int SCALE_DOWN_STEP = 3;
    private static final int SCALE_THRESHOLD = 3;

    private final Map<String, Integer> highLoadCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> lowLoadCounts = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 */2 * * * ?")
    public void autoAdjust() {
        adjustPool("eleOrderSyncExecutor", syncExecutor.getThreadPoolExecutor());
        adjustPool("eleStoreGoodsFullSyncExecutor", fullSyncExecutor.getThreadPoolExecutor());
        adjustPool("eleStoreGoodsPageExecutor", pageExecutor);
    }

    public void scaleUp(String poolName, int targetCoreSize) {
        scaleUp(resolvePool(poolName), poolName, targetCoreSize);
    }

    public void scaleDown(String poolName, int targetCoreSize) {
        scaleDown(resolvePool(poolName), poolName, targetCoreSize);
    }

    public void scaleUp(int targetCoreSize) {
        scaleUp("eleOrderSyncExecutor", targetCoreSize);
    }

    public void scaleDown(int targetCoreSize) {
        scaleDown("eleOrderSyncExecutor", targetCoreSize);
    }

    private ThreadPoolExecutor resolvePool(String poolName) {
        if ("eleOrderSyncExecutor".equals(poolName)) {
            return syncExecutor.getThreadPoolExecutor();
        }
        if ("eleStoreGoodsFullSyncExecutor".equals(poolName)) {
            return fullSyncExecutor.getThreadPoolExecutor();
        }
        if ("eleStoreGoodsPageExecutor".equals(poolName)) {
            return pageExecutor;
        }
        throw new IllegalArgumentException("不支持的线程池: " + poolName);
    }

    private void adjustPool(String poolName, ThreadPoolExecutor executor) {
        BlockingQueue<?> queue = executor.getQueue();
        int queueSize = queue.size();
        int queueCapacity = queue.remainingCapacity() + queueSize;
        double queueUsage = queueCapacity > 0 ? (queueSize * 100.0 / queueCapacity) : 0;
        int activeCount = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        double activePercent = poolSize > 0 ? (activeCount * 100.0 / poolSize) : 0;
        int currentCoreSize = executor.getCorePoolSize();
        int currentMaxSize = executor.getMaximumPoolSize();

        log.debug("[自适应线程池] {} 当前状态: coreSize={}, maxSize={}, queueUsage={}%, activePercent={}",
                poolName,
                currentCoreSize,
                currentMaxSize,
                String.format("%.1f", queueUsage),
                String.format("%.1f", activePercent));

        if (queueUsage >= QUEUE_HIGH_THRESHOLD || activePercent >= ACTIVE_HIGH_THRESHOLD) {
            lowLoadCounts.put(poolName, 0);
            int highLoadCount = highLoadCounts.merge(poolName, 1, Integer::sum);
            if (highLoadCount >= SCALE_THRESHOLD && currentCoreSize < MAX_CORE_SIZE) {
                int newCoreSize = Math.min(currentCoreSize + SCALE_UP_STEP, MAX_CORE_SIZE);
                int newMaxSize = Math.min(newCoreSize + SCALE_UP_STEP * 2, MAX_POOL_SIZE);
                executor.setMaximumPoolSize(newMaxSize);
                executor.setCorePoolSize(newCoreSize);
                highLoadCounts.put(poolName, 0);
                log.warn("[自适应线程池] {} 扩容触发: queueUsage={}%, activePercent={}, coreSize: {}->{}, maxSize: {}->{}",
                        poolName,
                        String.format("%.1f", queueUsage),
                        String.format("%.1f", activePercent),
                        currentCoreSize, newCoreSize,
                        currentMaxSize, newMaxSize);
            } else if (highLoadCount >= SCALE_THRESHOLD) {
                log.warn("[自适应线程池] {} 已达最大容量 {}, 无法继续扩容", poolName, MAX_CORE_SIZE);
            }
            return;
        }

        if (queueUsage < QUEUE_LOW_THRESHOLD && activePercent < ACTIVE_LOW_THRESHOLD) {
            highLoadCounts.put(poolName, 0);
            int lowLoadCount = lowLoadCounts.merge(poolName, 1, Integer::sum);
            if (lowLoadCount >= SCALE_THRESHOLD && currentCoreSize > MIN_CORE_SIZE) {
                int newCoreSize = Math.max(currentCoreSize - SCALE_DOWN_STEP, MIN_CORE_SIZE);
                int newMaxSize = newCoreSize + SCALE_UP_STEP * 2;
                executor.setMaximumPoolSize(newMaxSize);
                executor.setCorePoolSize(newCoreSize);
                lowLoadCounts.put(poolName, 0);
                log.info("[自适应线程池] {} 缩容触发: queueUsage={}%, activePercent={}, coreSize: {}->{}, maxSize: {}->{}",
                        poolName,
                        String.format("%.1f", queueUsage),
                        String.format("%.1f", activePercent),
                        currentCoreSize, newCoreSize,
                        currentMaxSize, newMaxSize);
            }
            return;
        }

        highLoadCounts.put(poolName, 0);
        lowLoadCounts.put(poolName, 0);
    }

    private void scaleUp(ThreadPoolExecutor executor, String poolName, int targetCoreSize) {
        int currentCoreSize = executor.getCorePoolSize();
        int currentMaxSize = executor.getMaximumPoolSize();
        int newCoreSize = Math.min(Math.max(targetCoreSize, currentCoreSize + 1), MAX_CORE_SIZE);
        int newMaxSize = newCoreSize + SCALE_UP_STEP * 2;
        executor.setMaximumPoolSize(newMaxSize);
        executor.setCorePoolSize(newCoreSize);
        log.info("[自适应线程池] {} 手动扩容: coreSize: {}->{}, maxSize: {}->{}",
                poolName, currentCoreSize, newCoreSize, currentMaxSize, newMaxSize);
    }

    private void scaleDown(ThreadPoolExecutor executor, String poolName, int targetCoreSize) {
        int currentCoreSize = executor.getCorePoolSize();
        int currentMaxSize = executor.getMaximumPoolSize();
        int newCoreSize = Math.max(targetCoreSize, MIN_CORE_SIZE);
        int newMaxSize = newCoreSize + SCALE_UP_STEP * 2;
        executor.setMaximumPoolSize(newMaxSize);
        executor.setCorePoolSize(newCoreSize);
        log.info("[自适应线程池] {} 手动缩容: coreSize: {}->{}, maxSize: {}->{}",
                poolName, currentCoreSize, newCoreSize, currentMaxSize, newMaxSize);
    }
}
