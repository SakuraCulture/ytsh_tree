package cn.iocoder.yudao.module.ele.service.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public class TaskRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TaskRateLimiter.class);

    
    private final Semaphore maxConcurrentSemaphore;

    
    private final AtomicInteger currentConcurrentTasks = new AtomicInteger(0);

    
    private volatile int maxConcurrentTasks;

    public TaskRateLimiter() {
        this.maxConcurrentTasks = 50;
        this.maxConcurrentSemaphore = new Semaphore(maxConcurrentTasks);
    }

    
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

    
    public void release() {
        int current = currentConcurrentTasks.decrementAndGet();
        maxConcurrentSemaphore.release();
        log.debug("[限流器] 任务完成释放许可, 当前并发数: {}", current);
    }

    
    public void setMaxConcurrent(int newMax) {
        int oldMax = this.maxConcurrentTasks;
        this.maxConcurrentTasks = newMax;
        log.info("[限流器] 调整最大并发数: {} -> {}", oldMax, newMax);
    }

    
    public int getCurrentConcurrentTasks() {
        return currentConcurrentTasks.get();
    }

    
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
}
