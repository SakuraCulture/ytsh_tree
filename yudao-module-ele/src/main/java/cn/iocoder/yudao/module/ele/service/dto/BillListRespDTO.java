package cn.iocoder.yudao.module.ele.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BillListRespDTO {
    private String errno;
    private String error;
    
    @JsonProperty("total")
    private Long total;
    
    @JsonProperty("bill_details")
    private List<BillDetailDTO> billDetails;
    
    @Data
    public static class BillDetailDTO {
        private String createTime;
        private String updateTime;
        private String merchantCode;
        private String storeCode;
        private String storeName;
        private String shopId;
        private String channelType;
        private String billDate;
        private String orderDate;
        private String orderId;
        private Long itemPrice;
        private Long packageFee;
        private Long deliveryFee;
        private Long shopMarketingFee;
        private Long platformFee;
        private Long donationFee;
        private Long billAmount;
        private Integer status;
        private String billTypeDesc;
        private String refundId;
        private String settleOrderId;
        private String shippingType;
        private Long userPayShippingAmount;
        private Long userOnlinePayAmount;
        private Long productPreferences;
        private Long notProductPreferences;
        private Long performanceServiceFee;
        private Long platformChargeFee;
        private String activityAmount;
    }
}
