package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@TableName("order_bill_table")
@Data
public class OrderBillDO {

    @TableId
    private Long id;

    private String billId;

    private String orderId;

    private LocalDate orderDate;

    private String refundId;

    private String merchantCode;

    private String storeCode;

    private String shopId;

    private String storeName;

    private String channelType;

    private LocalDate billDate;

    private Integer status;

    private Long billAmount;

    private Long itemPrice;

    private Long packageFee;

    private Long deliveryFee;

    private Long shopMarketingFee;

    private Long platformFee;

    private Long donationFee;

    private Long userPayShippingAmount;

    private Long userOnlinePayAmount;

    private Long productPreferences;

    private Long notProductPreferences;

    private Long performanceServiceFee;

    private Long platformChargeFee;

    private Long activityAmount;

    private String billTypeDesc;

    private String shippingType;

    private String settleOrderId;

    private Date syncTime;

    private Long createTime;

    private Long updateTime;

    private String tenantId;

    private Boolean deleted;
}
