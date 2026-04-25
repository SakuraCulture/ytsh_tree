package cn.iocoder.yudao.module.ele.controller.admin.order.vo;

import lombok.Data;

@Data
public class SubOrderItemVO {

    private String skuCode;
    private String skuName;
    private String barcode;
    private String specification;
    private java.math.BigDecimal weight;
    private Integer buyAmount;
    private Integer price;
    private Integer totalFee;
    private Integer payFee;
}
