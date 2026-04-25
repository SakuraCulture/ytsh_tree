package cn.iocoder.yudao.module.ele.controller.admin.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class EleOrderItemVO {

    private String orderId;
    private Integer status;
    private Long createTime;
    private Long payTime;
    private String channelSourceName;
    private String buyerName;
    private String buyerPhone;
    private String buyerAddress;
    private String deliveryName;
    private String deliveryPhone;
    private String deliveryPlatform;
    private Integer deliveryType;
    private Integer deliveryStatus;
    private Integer totalFee;
    private Integer payFee;
    private Integer discountFee;
    private Integer deliveryFee;
    private Integer postFee;
    private Integer packageFee;
    private Integer platformCommissionFee;
    private String remark;
    private String channelSourceId;
    private String channelOrderId;
    private String channelType;
    private String storeCode;
    private String erpStoreCode;
    private String longitude;
    private String latitude;
    private List<SubOrderItemVO> subOrders;
    private List<DiscountItemVO> discounts;
}
