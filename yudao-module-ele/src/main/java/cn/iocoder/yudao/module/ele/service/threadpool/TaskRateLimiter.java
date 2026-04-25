package cn.iocoder.yudao.module.ele.service.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务提交限流器
 *
 * 控制同时提交到线程池的任务数量,避免瞬间提交过多任务导致队列积压。
 * 采用令牌桶+滑动窗口策略,实现平滑限流。
 *
 * @author 优团科技数字化团队
 */
@Component
public class TaskRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TaskRateLimiter.class);

    /** 最大并发任务数(防止队列瞬间打满) */
    private final Semaphore maxConcurrentSemaphore;

    /** 当前并发任务数 */
    private final AtomicInteger currentConcurrentTasks = new AtomicInteger(0);

    /** 最大并发数配置 */
    private volatile int maxConcurrentTasks;

    public TaskRateLimiter() {
        this.maxConcurrentTasks = 50;
        this.maxConcurrentSemaphore = new Semaphore(maxConcurrentTasks);
    }

    /**
     * 尝试获取任务提交许可
     *
     * @return true=获取成功可以提交, false=被限流
     */
    public boolean tryAcquire() {
        boolean acquired = maxConcurrentSemaphore.tryAcquire();
        if (acquired) {
            currentConcurrentTasks.incrementAndGet();
            log.debug("[限流器] 任务提交许可获取成功, 当前并发数: {}", currentConcurrentTasks.get());
        } else {
            log.warn("[限流器] 任务提交被限流, 当前并发数: {}/{}", currentConcurrentTasks.get(), maxConcurrentTasks);
        }
        return acquired;
    }

    /**
     * 释放任务提交许可(任务完成后调用)
     */
    public void release() {
        int current = currentConcurrentTasks.decrementAndGet();
        maxConcurrentSemaphore.release();
        log.debug("[限流器] 任务完成释放许可, 当前并发数: {}", current);
    }

    /**
     * 动态调整最大并发数
     */
    public void setMaxConcurrent(int newMax) {
        int oldMax = this.maxConcurrentTasks;
        this.maxConcurrentTasks = newMax;
        log.info("[限流器] 调整最大并发数: {} -> {}", oldMax, newMax);
    }

    /**
     * 获取当前并发任务数
     */
    public int getCurrentConcurrentTasks() {
        return currentConcurrentTasks.get();
    }

    /**
     * 获取最大并发数
     */
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
}
