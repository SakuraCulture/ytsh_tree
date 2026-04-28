package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class CompensationTaskResult {
    private String orderId;
    private String platformStoreId;
    private String merchantCode;
    private String erpStoreCode;
    private boolean success;
    private String errorMessage;
    private Exception exception;
}
