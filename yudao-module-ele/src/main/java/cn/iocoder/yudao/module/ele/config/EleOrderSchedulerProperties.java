package cn.iocoder.yudao.module.ele.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 饿了么订单定时同步调度配置
 *
 * @author 优团科技数字化团队
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ele.order.scheduler")
public class EleOrderSchedulerProperties {

    /**
     * 是否启用定时同步
     */
    private boolean enabled = true;

    /**
     * 同步间隔（秒），默认 3600 秒（1小时）
     */
    private long intervalSeconds = 3600;

    /**
     * 初始延迟（秒），默认 60 秒
     */
    private long initialDelaySeconds = 60;

}
