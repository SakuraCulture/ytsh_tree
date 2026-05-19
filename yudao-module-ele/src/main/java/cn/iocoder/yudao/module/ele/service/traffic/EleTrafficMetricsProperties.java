package cn.iocoder.yudao.module.ele.service.traffic;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ele.traffic.metrics")
public class EleTrafficMetricsProperties {

    /** 是否启用翱象接口流量采集 */
    private boolean enabled = true;

    /** 是否记录实时明细；大批量拉单时建议关闭，仅保留聚合指标 */
    private boolean realtimeRecordEnabled = false;

    /** 聚合数据刷入 Redis 的间隔 */
    private long flushIntervalMs = 5000;

    /** Redis 异常后进入降级状态的恢复探测间隔 */
    private long degradationCheckIntervalMs = 30000;

    /** 采集数据保留天数 */
    private int retentionDays = 3;
}
