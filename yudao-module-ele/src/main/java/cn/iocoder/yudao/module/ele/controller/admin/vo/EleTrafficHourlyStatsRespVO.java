package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 流量监控小时统计 Response VO")
@Data
public class EleTrafficHourlyStatsRespVO {

    @Schema(description = "小时(00-23)")
    private String hour;

    @Schema(description = "请求数")
    private Long requests;

    @Schema(description = "请求字节数")
    private Long requestBytes;

    @Schema(description = "响应字节数")
    private Long responseBytes;

    @Schema(description = "平均耗时(ms)")
    private Double avgDurationMs;

    @Schema(description = "成功数")
    private Long successCount;

    @Schema(description = "失败数")
    private Long failedCount;
}
