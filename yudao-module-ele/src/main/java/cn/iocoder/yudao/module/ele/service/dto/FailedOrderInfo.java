package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class FailedOrderInfo {
    private String orderId;
    private String channelOrderId;
    private String platformStoreId;
    private String merchantCode;
    private String erpStoreCode;
    private String errorMessage;
    private Long failTimestamp;
    private Object orderDetail;
    private int retryCount;
    private boolean compensated;
    private String compensationResult;
}
