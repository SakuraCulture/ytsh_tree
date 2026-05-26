package cn.iocoder.yudao.module.ele.controller.admin.vo;

import lombok.Data;

@Data
public class EleOrderTrackingAlertVO {
    
    private Long id;

    
    private String orderId;

    
    private String platformStoreId;

    
    private String erpStoreCode;

    
    private Integer orderStatus;

    
    private String alertLevel;

    
    private Long createTime;

    
    private String createTimeStr;

    
    private Integer daysElapsed;

    
    private String remark;
}
