package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDetailRespDTO {

    private String orderId;
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
    private Integer status;
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
    private String userId;
    private Integer arriveType;
    private List<SubOrder> subOrders = new ArrayList<>();
    private List<Discount> discounts = new ArrayList<>();

    @Data
    public static class SubOrder {
        private String subOrderId;
        private String skuCode;
        private String skuName;
        private String barcode;
        private String specification;
        private Integer price;
        private Integer totalFee;
        private Integer payFee;
        private Integer buyAmount;
        private String goodsType;
        private String cabinetCode;
        private Integer weight;
        private Integer num;
    }

    @Data
    public static class Discount {
        private String activityName;
        private Integer activityOrderType;
        private String activityId;
        private String type;
        private Integer discountFee;
        private Integer merchantFee;
        private Integer platformFee;
    }
}
