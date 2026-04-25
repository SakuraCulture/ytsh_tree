package cn.iocoder.yudao.server.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/**
 * 开发环境显式接管 RedissonClient，确保使用 application-dev.yaml 中的 redisson 配置。
 */
@Configuration
@Profile("dev")
public class DevRedissonConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.redisson.config}") String redissonConfig) throws IOException {
        Config config = Config.fromYAML(redissonConfig);
        return Redisson.create(config);
    }

}
