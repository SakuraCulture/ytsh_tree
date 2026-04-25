package cn.iocoder.yudao.module.system.framework.captcha.core;

import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import com.anji.captcha.service.CaptchaCacheService;
import lombok.Setter;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 实现验证码的存储
 *
 * @author 星语
 */
@Setter
public class RedisCaptchaServiceImpl implements CaptchaCacheService {

    private StringRedisTemplate stringRedisTemplate;

    private StringRedisTemplate getStringRedisTemplate() {
        if (stringRedisTemplate == null) {
            stringRedisTemplate = SpringUtils.getBean(StringRedisTemplate.class);
        }
        return stringRedisTemplate;
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        getStringRedisTemplate().opsForValue().set(key, value, expiresInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(String key) {
        return getStringRedisTemplate().hasKey(key);
    }

    @Override
    public void delete(String key) {
        getStringRedisTemplate().delete(key);
    }

    @Override
    public String get(String key) {
        return getStringRedisTemplate().opsForValue().get(key);
    }

    @Override
    public Long increment(String key, long val) {
        return getStringRedisTemplate().opsForValue().increment(key, val);
    }

}
