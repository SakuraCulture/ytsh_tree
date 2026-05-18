package cn.iocoder.yudao.module.ele.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 饿了么订单同步线程池配置
 *
 * @author 优团科技数字化团队
 */
@Configuration
public class EleOrderExecutorConfiguration {

    @Bean(name = "eleOrderSyncExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eleOrderSyncExecutor(
            @Value("${ele.order.sync.pool.core-size:10}") int coreSize,
            @Value("${ele.order.sync.pool.max-size:20}") int maxSize,
            @Value("${ele.order.sync.pool.queue-capacity:200}") int queueCapacity,
            @Value("${ele.order.sync.pool.keep-alive-seconds:60}") int keepAliveSeconds,
            @Value("${ele.order.sync.shutdown.await-termination:true}") boolean awaitTermination,
            @Value("${ele.order.sync.shutdown.await-termination-seconds:60}") int awaitTerminationSeconds) {
        return buildExecutor(coreSize, maxSize, queueCapacity, keepAliveSeconds, awaitTermination,
                awaitTerminationSeconds, "ele-order-sync-");
    }

    @Bean(name = "eleOrderDetailEnrichExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eleOrderDetailEnrichExecutor(
            @Value("${ele.order.detail.pool.core-size:3}") int coreSize,
            @Value("${ele.order.detail.pool.max-size:5}") int maxSize,
            @Value("${ele.order.detail.pool.queue-capacity:200}") int queueCapacity,
            @Value("${ele.order.detail.pool.keep-alive-seconds:60}") int keepAliveSeconds,
            @Value("${ele.order.sync.shutdown.await-termination:true}") boolean awaitTermination,
            @Value("${ele.order.sync.shutdown.await-termination-seconds:60}") int awaitTerminationSeconds) {
        return buildExecutor(coreSize, maxSize, queueCapacity, keepAliveSeconds, awaitTermination,
                awaitTerminationSeconds, "ele-order-detail-enrich-");
    }

    @Bean(name = "eleStoreGoodsFullSyncExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eleStoreGoodsFullSyncExecutor(
            @Value("${ele.store.goods.full-sync.pool.core-size:12}") int coreSize,
            @Value("${ele.store.goods.full-sync.pool.max-size:24}") int maxSize,
            @Value("${ele.store.goods.full-sync.pool.queue-capacity:0}") int queueCapacity,
            @Value("${ele.store.goods.full-sync.pool.keep-alive-seconds:60}") int keepAliveSeconds,
            @Value("${ele.store.goods.full-sync.shutdown.await-termination:true}") boolean awaitTermination,
            @Value("${ele.store.goods.full-sync.shutdown.await-termination-seconds:120}") int awaitTerminationSeconds) {
        return buildExecutor(coreSize, maxSize, queueCapacity, keepAliveSeconds, awaitTermination,
                awaitTerminationSeconds, "ele-store-goods-full-sync-");
    }

    @Bean(name = "eleStoreInventoryBatchExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eleStoreInventoryBatchExecutor(
            @Value("${ele.store.inventory.batch.pool.core-size:12}") int coreSize,
            @Value("${ele.store.inventory.batch.pool.max-size:24}") int maxSize,
            @Value("${ele.store.inventory.batch.pool.queue-capacity:0}") int queueCapacity,
            @Value("${ele.store.inventory.batch.pool.keep-alive-seconds:60}") int keepAliveSeconds,
            @Value("${ele.store.inventory.batch.shutdown.await-termination:true}") boolean awaitTermination,
            @Value("${ele.store.inventory.batch.shutdown.await-termination-seconds:120}") int awaitTerminationSeconds) {
        return buildExecutor(coreSize, maxSize, queueCapacity, keepAliveSeconds, awaitTermination,
                awaitTerminationSeconds, "ele-store-inventory-batch-");
    }

    private ThreadPoolTaskExecutor buildExecutor(int coreSize, int maxSize, int queueCapacity,
                                                 int keepAliveSeconds, boolean awaitTermination,
                                                 int awaitTerminationSeconds, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(awaitTermination);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }
}
