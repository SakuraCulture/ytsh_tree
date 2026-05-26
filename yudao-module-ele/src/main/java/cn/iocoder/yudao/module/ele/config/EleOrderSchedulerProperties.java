package cn.iocoder.yudao.module.ele.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "ele.order.scheduler")
public class EleOrderSchedulerProperties {

    
    private boolean enabled = true;

    
    private long intervalSeconds = 3600;

    
    private long initialDelaySeconds = 60;

}
