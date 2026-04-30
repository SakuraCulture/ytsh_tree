package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class OrderPushLogDTO {
    private String orderId;
    private String channelOrderId;
    private String merchantCode;
    private String erpStoreCode;
    private Integer status;
    private String statusName;
    private String ticket;
    private Long pushTime;
    private String pushTimeStr;
    private String receiveStatus;
    private String consumeStatus;
    private String errorMessage;
    private String kafkaPartition;
    private String kafkaOffset;
    private String websocketPushStatus;
    private Long consumeTime;
    private String consumeTimeStr;
}
