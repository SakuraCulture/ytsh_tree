package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 分接口实时RPS响应")
@Data
public class EleTrafficApiRpsRespVO {

    @Schema(description = "接口编码", example = "ORDER_LIST")
    private String apiCode;

    @Schema(description = "接口名称", example = "订单列表查询")
    private String apiName;

    @Schema(description = "接口路径", example = "/ele/order/list")
    private String apiPath;

    @Schema(description = "当前每秒请求数", example = "45")
    private Integer currentRps;

    @Schema(description = "近5秒平均RPS", example = "38")
    private Integer avgRps5s;

    @Schema(description = "近60秒峰值RPS", example = "120")
    private Integer maxRps60s;

    @Schema(description = "总请求数", example = "12345")
    private Long totalRequests;

    @Schema(description = "统计时间戳", example = "1714032000000")
    private Long timestamp;

    @Schema(description = "近60秒RPS历史")
    private List<Integer> rpsHistory;
}
