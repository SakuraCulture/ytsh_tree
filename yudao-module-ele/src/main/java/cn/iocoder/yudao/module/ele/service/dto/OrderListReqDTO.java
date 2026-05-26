package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;


@Data
public class OrderListReqDTO {

    
    private String platformStoreId;

    
    private String merchantCode;

    
    private String erpStoreCode;

    
    private Integer status;

    
    private Long startTime;

    
    private Long endTime;

    
    private Integer pageSize = 20;

    
    private String scrollId;
}