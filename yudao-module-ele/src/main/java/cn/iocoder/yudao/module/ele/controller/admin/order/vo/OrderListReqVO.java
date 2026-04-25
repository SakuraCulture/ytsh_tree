package cn.iocoder.yudao.module.ele.controller.admin.order.vo;

import lombok.Data;

@Data
public class OrderListReqVO {

    private String platformStoreId;
    private String merchantCode;
    private String erpStoreCode;
    private Integer status;
    private Long startTime;
    private Long endTime;
}
