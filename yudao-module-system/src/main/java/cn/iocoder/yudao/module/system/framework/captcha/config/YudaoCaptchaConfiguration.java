package cn.iocoder.yudao.module.system.framework.captcha.config;

import cn.iocoder.yudao.module.system.framework.captcha.core.RedisCaptchaServiceImpl;
import com.anji.captcha.config.AjCaptchaAutoConfiguration;
import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 验证码的配置类
 *
 * @author 优团科技数字化团队
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(AjCaptchaAutoConfiguration.class) // 目的：解决 aj-captcha 针对 SpringBoot 3.X 自动配置不生效的问题
public class YudaoCaptchaConfiguration {

    @Bean(name = "AjCaptchaCacheService")
    @Primary
    public CaptchaCacheService captchaCacheService(AjCaptchaProperties config,
                                                   StringRedisTemplate stringRedisTemplate) {
        String cacheType = String.valueOf(config.getCacheType());
        if ("redis".equalsIgnoreCase(cacheType)) {
            RedisCaptchaServiceImpl captchaCacheService = new RedisCaptchaServiceImpl();
            captchaCacheService.setStringRedisTemplate(stringRedisTemplate);
            return captchaCacheService;
        }
        return CaptchaServiceFactory.getCache(cacheType);
    }

}
