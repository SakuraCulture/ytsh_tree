package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class OrderRetryMessage {
    private String orderId;
    private String channelOrderId;
    private String platformStoreId;
    private String merchantCode;
    private String erpStoreCode;
    private Long failRecordId;
    private Integer retryCount;
    private Long createTime;
    private String traceId;
}
