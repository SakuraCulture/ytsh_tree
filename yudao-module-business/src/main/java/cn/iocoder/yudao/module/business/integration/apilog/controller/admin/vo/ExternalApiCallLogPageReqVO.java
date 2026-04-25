package cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 外部 API 调用日志分页 Request VO")
public class ExternalApiCallLogPageReqVO extends PageParam {

    @Schema(description = "平台编码", example = "ELE")
    private String platformCode;

    @Schema(description = "接口编码", example = "ORDER_LIST")
    private String apiCode;

    @Schema(description = "业务类型", example = "ORDER")
    private String bizType;

    @Schema(description = "业务 ID", example = "order-1")
    private String bizId;

    @Schema(description = "链路 ID", example = "trace-1")
    private String traceId;

    @Schema(description = "平台 traceId", example = "ext-trace-1")
    private String externalTraceId;

    @Schema(description = "订单号", example = "order-1")
    private String orderId;
}
