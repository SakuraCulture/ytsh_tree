package cn.iocoder.yudao.module.ele.service.threadpool;

import cn.iocoder.yudao.module.ele.service.threadpool.ThreadPoolAlarmConfigService.PoolAlarmConfig;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ThreadPoolHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolHealthChecker.class);

    @Resource
    private ThreadPoolRegistry threadPoolRegistry;

    @Resource
    private ThreadPoolAlarmConfigService alarmConfigService;

    @Scheduled(cron = "0 */2 * * * ?")
    public void checkAndLog() {
        Map<String, ThreadPoolTaskExecutor> pools = threadPoolRegistry.getAllPools();
        
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : pools.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolTaskExecutor executor = entry.getValue();
            ThreadPoolExecutor inner = executor.getThreadPoolExecutor();
            
            int queueSize = inner.getQueue().size();
            int queueCapacity = executor.getQueueCapacity();
            double queueUsage = queueCapacity > 0 ? (queueSize * 100.0 / queueCapacity) : 0;
            int activeCount = inner.getActiveCount();
            int poolSize = inner.getPoolSize();
            double activePercent = poolSize > 0 ? (activeCount * 100.0 / poolSize) : 0;
            long completedTaskCount = inner.getCompletedTaskCount();
            int largestPoolSize = inner.getLargestPoolSize();
            
            PoolAlarmConfig config = alarmConfigService.getAlarmConfig(poolName);
            
            if (!config.isEnabled()) {
                continue;
            }
            
            if (queueUsage >= config.getQueueThresholdPercent() || activePercent >= config.getActiveThresholdPercent()) {
                log.warn("[线程池报警] poolName={}, 队列使用={}% (阈值{}%), 活跃率={}% (阈值{}%), 队列={}/{}",
                        poolName,
                        String.format("%.1f", queueUsage), config.getQueueThresholdPercent(),
                        String.format("%.1f", activePercent), config.getActiveThresholdPercent(),
                        queueSize, queueCapacity);
            }
            
                                                boolean criticalByBoth = queueUsage >= 80 && activePercent >= 90;
            boolean criticalByQueue = queueUsage >= 95;
            
            if (criticalByBoth || criticalByQueue) {
                log.error("[线程池严重告警] poolName={}, 队列使用={}% (容量:{}/{}), 活跃率={}% (活跃:{}/{})" +
                        ", 历史最大线程={}, 已完成任务={}, 请立即处理!",
                        poolName,
                        String.format("%.1f", queueUsage),
                        queueSize, queueCapacity,
                        String.format("%.1f", activePercent),
                        activeCount, poolSize,
                        largestPoolSize,
                        completedTaskCount);
            }
        }
    }
}
