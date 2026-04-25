package cn.iocoder.yudao.module.ele.controller.admin.order.vo;

import lombok.Data;

@Data
public class DiscountItemVO {

    private String activityName;
    private String type;
    private String typeName;
    private Integer discountFee;
    private Integer merchantFee;
    private Integer platformFee;
}
