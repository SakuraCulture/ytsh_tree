package cn.iocoder.yudao.framework.redis.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Slf4j
public class RedisHealthChecker {

    private final RedisConnectionFactory connectionFactory;
    private final RedissonClient redissonClient;

    public RedisHealthChecker(RedisConnectionFactory connectionFactory, RedissonClient redissonClient) {
        this.connectionFactory = connectionFactory;
        this.redissonClient = redissonClient;
    }

    public boolean waitForRedisReady(int maxWaitSeconds) {
        int waited = 0;
        while (waited < maxWaitSeconds) {
            try {
                connectionFactory.getConnection().ping();
                log.info("Redis 连接正常，PING 成功");
                return true;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("LOADING")) {
                    log.warn("Redis 正在加载数据，已等待 {} 秒，2秒后重试...", waited);
                    try {
                        Thread.sleep(2000);
                        waited += 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    log.error("Redis 连接异常: {}", e.getMessage());
                    try {
                        Thread.sleep(2000);
                        waited += 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }
}
