package cn.iocoder.yudao.module.ele.service.traffic;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ele.traffic.metrics")
public class EleTrafficMetricsProperties {

    
    private boolean enabled = true;

    
    private boolean realtimeRecordEnabled = false;

    
    private long flushIntervalMs = 5000;

    
    private long degradationCheckIntervalMs = 30000;

    
    private int retentionDays = 3;
}
