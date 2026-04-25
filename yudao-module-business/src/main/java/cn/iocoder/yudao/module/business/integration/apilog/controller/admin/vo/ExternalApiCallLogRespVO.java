package cn.iocoder.yudao.module.business.integration.apilog.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理后台 - 外部 API 调用日志 Response VO")
public class ExternalApiCallLogRespVO {

    private Long id;
    private String platformCode;
    private String apiCode;
    private String apiName;
    private String bizType;
    private String bizId;
    private String bizNo;
    private String traceId;
    private String externalTraceId;
    private String requestId;
    private String requestUrl;
    private String requestMethod;
    private String requestBody;
    private String responseBody;
    private Boolean success;
    private String resultCode;
    private String resultMsg;
    private Integer durationMs;
    private String merchantCode;
    private String platformStoreId;
    private String erpStoreCode;
    private String orderId;
    private String channelOrderId;
    private LocalDateTime createTime;
}
