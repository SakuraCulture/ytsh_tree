package cn.iocoder.yudao.server.config.redis;

import cn.iocoder.yudao.framework.redis.core.RedisHealthChecker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class RedisHealthCheckConfiguration {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        log.info("开始执行 Redis 健康检查...");
        
        RedisHealthChecker healthChecker = new RedisHealthChecker(
                redisConnectionFactory, 
                redissonClient
        );
        
        boolean isReady = healthChecker.waitForRedisReady(30);
        
        if (isReady) {
            log.info("Redis 健康检查通过，服务已就绪");
        } else {
            log.error("Redis 健康检查失败，30秒内未能完成数据加载");
        }
    }
}
