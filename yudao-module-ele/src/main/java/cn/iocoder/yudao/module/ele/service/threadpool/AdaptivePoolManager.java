package cn.iocoder.yudao.module.ele.service.threadpool;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

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

    /** 核心线程数下限 */
    private static final int MIN_CORE_SIZE = 10;

    /** 核心线程数上限 */
    private static final int MAX_CORE_SIZE = 50;

    /** 最大线程数上限 */
    private static final int MAX_POOL_SIZE = 80;

    /** 队列使用率告警阈值(%) - 超过此值考虑扩容 */
    private static final int QUEUE_HIGH_THRESHOLD = 60;

    /** 队列使用率恢复阈值(%) - 低于此值可缩容 */
    private static final int QUEUE_LOW_THRESHOLD = 30;

    /** 活跃率告警阈值(%) - 超过此值考虑扩容 */
    private static final int ACTIVE_HIGH_THRESHOLD = 85;

    /** 活跃率恢复阈值(%) - 低于此值可缩容 */
    private static final int ACTIVE_LOW_THRESHOLD = 50;

    /** 扩容步长 */
    private static final int SCALE_UP_STEP = 5;

    /** 缩容步长 */
    private static final int SCALE_DOWN_STEP = 3;

    /** 连续触发次数 - 避免抖动 */
    private int highLoadCount = 0;
    private int lowLoadCount = 0;

    /** 连续触发阈值 */
    private static final int SCALE_THRESHOLD = 3;

    /**
     * 每2分钟检查一次并自动调整
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void autoAdjust() {
        ThreadPoolExecutor executor = syncExecutor.getThreadPoolExecutor();

        int queueSize = executor.getQueue().size();
        int queueCapacity = syncExecutor.getQueueCapacity();
        double queueUsage = queueCapacity > 0 ? (queueSize * 100.0 / queueCapacity) : 0;
        int activeCount = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        double activePercent = poolSize > 0 ? (activeCount * 100.0 / poolSize) : 0;
        int currentCoreSize = syncExecutor.getCorePoolSize();
        int currentMaxSize = syncExecutor.getMaxPoolSize();

        log.debug("[自适应线程池] 当前状态: coreSize={}, maxSize={}, queueUsage={}%, activePercent={}",
                currentCoreSize, currentMaxSize,
                String.format("%.1f", queueUsage),
                String.format("%.1f", activePercent));

        if (queueUsage >= QUEUE_HIGH_THRESHOLD || activePercent >= ACTIVE_HIGH_THRESHOLD) {
            lowLoadCount = 0;
            highLoadCount++;

            if (highLoadCount >= SCALE_THRESHOLD && currentCoreSize < MAX_CORE_SIZE) {
                int newCoreSize = Math.min(currentCoreSize + SCALE_UP_STEP, MAX_CORE_SIZE);
                int newMaxSize = Math.min(newCoreSize + SCALE_UP_STEP * 2, MAX_POOL_SIZE);

                syncExecutor.setMaxPoolSize(newMaxSize);
                syncExecutor.setCorePoolSize(newCoreSize);
                syncExecutor.afterPropertiesSet();

                log.warn("[自适应线程池] 扩容触发: queueUsage={}%, activePercent={}, coreSize: {}->{}, maxSize: {}->{}",
                        String.format("%.1f", queueUsage),
                        String.format("%.1f", activePercent),
                        currentCoreSize, newCoreSize,
                        currentMaxSize, newMaxSize);
            } else if (highLoadCount >= SCALE_THRESHOLD) {
                log.warn("[自适应线程池] 已达最大容量 {}, 无法继续扩容", MAX_CORE_SIZE);
            }
        } else if (queueUsage < QUEUE_LOW_THRESHOLD && activePercent < ACTIVE_LOW_THRESHOLD) {
            highLoadCount = 0;
            lowLoadCount++;

            if (lowLoadCount >= SCALE_THRESHOLD && currentCoreSize > MIN_CORE_SIZE) {
                int newCoreSize = Math.max(currentCoreSize - SCALE_DOWN_STEP, MIN_CORE_SIZE);
                int newMaxSize = newCoreSize + SCALE_UP_STEP * 2;

                syncExecutor.setMaxPoolSize(newMaxSize);
                syncExecutor.setCorePoolSize(newCoreSize);
                syncExecutor.afterPropertiesSet();

                log.info("[自适应线程池] 缩容触发: queueUsage={}%, activePercent={}, coreSize: {}->{}, maxSize: {}->{}",
                        String.format("%.1f", queueUsage),
                        String.format("%.1f", activePercent),
                        currentCoreSize, newCoreSize,
                        currentMaxSize, newMaxSize);
            }
        } else {
            highLoadCount = 0;
            lowLoadCount = 0;
        }
    }

    /**
     * 手动扩容接口
     */
    public void scaleUp(int targetCoreSize) {
        int currentCoreSize = syncExecutor.getCorePoolSize();
        int newCoreSize = Math.min(Math.max(targetCoreSize, currentCoreSize + 1), MAX_CORE_SIZE);
        int newMaxSize = newCoreSize + SCALE_UP_STEP * 2;

        syncExecutor.setMaxPoolSize(newMaxSize);
        syncExecutor.setCorePoolSize(newCoreSize);
        syncExecutor.afterPropertiesSet();

        log.info("[自适应线程池] 手动扩容: coreSize: {}->{}, maxSize: {}->{}",
                currentCoreSize, newCoreSize,
                syncExecutor.getMaxPoolSize(), newMaxSize);
    }

    /**
     * 手动缩容接口
     */
    public void scaleDown(int targetCoreSize) {
        int currentCoreSize = syncExecutor.getCorePoolSize();
        int newCoreSize = Math.max(targetCoreSize, MIN_CORE_SIZE);
        int newMaxSize = newCoreSize + SCALE_UP_STEP * 2;

        syncExecutor.setMaxPoolSize(newMaxSize);
        syncExecutor.setCorePoolSize(newCoreSize);
        syncExecutor.afterPropertiesSet();

        log.info("[自适应线程池] 手动缩容: coreSize: {}->{}, maxSize: {}->{}",
                currentCoreSize, newCoreSize,
                syncExecutor.getMaxPoolSize(), newMaxSize);
    }
}
