package lib.ele.retail.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SaasBillListResult {
    private String errno;
    private String error;
    private SaasBillDetailResult data;

    @Data
    public static class SaasBillDetailResult {
        private Long total;

        @JsonProperty("bill_details")
        private List<BillDetailDTO> billDetails;
    }

    @Data
    public static class BillDetailDTO {
        @JsonProperty("create_time")
        private String createTime;
        @JsonProperty("update_time")
        private String updateTime;
        @JsonProperty("merchant_code")
        private String merchantCode;
        @JsonProperty("store_code")
        private String storeCode;
        @JsonProperty("store_name")
        private String storeName;
        @JsonProperty("shop_id")
        private String shopId;
        @JsonProperty("channel_type")
        private String channelType;
        @JsonProperty("bill_date")
        private String billDate;
        @JsonProperty("order_date")
        private String orderDate;
        @JsonProperty("order_id")
        private String orderId;
        @JsonProperty("item_price")
        private Long itemPrice;
        @JsonProperty("package_fee")
        private Long packageFee;
        @JsonProperty("delivery_fee")
        private Long deliveryFee;
        @JsonProperty("shop_marketing_fee")
        private Long shopMarketingFee;
        @JsonProperty("platform_fee")
        private Long platformFee;
        @JsonProperty("donation_fee")
        private Long donationFee;
        @JsonProperty("bill_amount")
        private Long billAmount;
        @JsonProperty("status")
        private Integer status;
        @JsonProperty("bill_type_desc")
        private String billTypeDesc;
        @JsonProperty("refund_id")
        private String refundId;
        @JsonProperty("settle_order_id")
        private String settleOrderId;
        @JsonProperty("shipping_type")
        private String shippingType;
        @JsonProperty("user_pay_shipping_amount")
        private Long userPayShippingAmount;
        @JsonProperty("user_online_pay_amount")
        private Long userOnlinePayAmount;
        @JsonProperty("product_preferences")
        private Long productPreferences;
        @JsonProperty("not_product_preferences")
        private Long notProductPreferences;
        @JsonProperty("performance_service_fee")
        private Long performanceServiceFee;
        @JsonProperty("platform_charge_fee")
        private Long platformChargeFee;
        @JsonProperty("activity_amount")
        private String activityAmount;
    }
}
