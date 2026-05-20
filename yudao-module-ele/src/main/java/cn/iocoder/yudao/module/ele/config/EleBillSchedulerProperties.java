package cn.iocoder.yudao.module.ele.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ele.bill.scheduler")
public class EleBillSchedulerProperties {

    private boolean enabled = true;

    private String time = "01:00";

    private int offsetDays = 1;
}
