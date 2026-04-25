package cn.iocoder.yudao.module.business.integration.apilog.service.bo;

import lombok.Data;

@Data
public class ExternalApiCallLogCreateReqBO {

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
}
