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

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);

        executor.setThreadNamePrefix("ele-order-sync-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(awaitTermination);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        executor.initialize();
        return executor;
    }

}
