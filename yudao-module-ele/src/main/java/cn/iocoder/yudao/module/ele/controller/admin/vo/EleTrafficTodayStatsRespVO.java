package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 流量监控当日统计 Response VO")
@Data
public class EleTrafficTodayStatsRespVO {

    @Schema(description = "总请求数")
    private Long totalRequests;

    @Schema(description = "总请求字节数")
    private Long totalRequestBytes;

    @Schema(description = "总响应字节数")
    private Long totalResponseBytes;

    @Schema(description = "平均请求字节数")
    private Long avgRequestBytes;

    @Schema(description = "平均响应字节数")
    private Long avgResponseBytes;

    @Schema(description = "成功率(%)")
    private Double successRate;

    @Schema(description = "平均耗时(ms)")
    private Double avgDurationMs;

    @Schema(description = "最大请求字节数")
    private Long maxRequestBytes;

    @Schema(description = "最大响应字节数")
    private Long maxResponseBytes;

    @Schema(description = "成功请求数")
    private Long successRequests;

    @Schema(description = "失败请求数")
    private Long failedRequests;
}
