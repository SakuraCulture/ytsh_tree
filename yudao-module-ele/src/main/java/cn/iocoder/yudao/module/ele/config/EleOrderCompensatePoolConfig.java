package cn.iocoder.yudao.module.ele.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 饿了么订单补偿任务线程池配置
 *
 * @author 优团科技数字化团队
 */
@Configuration
public class EleOrderCompensatePoolConfig {

    @Bean(name = "eleOrderCompensateExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eleOrderCompensateExecutor(
            @Value("${ele.order.compensate.pool.core-size:2}") int coreSize,
            @Value("${ele.order.compensate.pool.max-size:5}") int maxSize,
            @Value("${ele.order.compensate.pool.queue-capacity:100}") int queueCapacity,
            @Value("${ele.order.compensate.pool.keep-alive-seconds:60}") int keepAliveSeconds) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);

        executor.setThreadNamePrefix("ele-order-compensate-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

}
