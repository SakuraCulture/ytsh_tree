package cn.iocoder.yudao.module.ele.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 饿了么订单失败重试任务线程池配置
 *
 * 用于异步重试失败的订单落库任务，与主同步线程池隔离，避免重试任务占用同步资源。
 *
 * @author 优团科技数字化团队
 */
@Configuration
public class EleOrderRetryPoolConfig {

    @Bean(name = "eleOrderRetryExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eleOrderRetryExecutor(
            @Value("${ele.order.retry.pool.core-size:5}") int coreSize,
            @Value("${ele.order.retry.pool.max-size:10}") int maxSize,
            @Value("${ele.order.retry.pool.queue-capacity:500}") int queueCapacity,
            @Value("${ele.order.retry.pool.keep-alive-seconds:60}") int keepAliveSeconds) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);

        executor.setThreadNamePrefix("ele-order-retry-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

}
