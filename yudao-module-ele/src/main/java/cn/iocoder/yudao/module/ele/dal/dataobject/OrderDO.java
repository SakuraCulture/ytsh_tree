package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;


@TableName("order_table")
@Data
public class OrderDO {

    
    @TableId
    private String orderId;

    
    private String userId;

    
    private Integer orderStatus;

    
    private Long createTime;

    
    private Long payTime;

    
    private String buyerName;

    
    private String buyerPhone;

    
    private String buyerAddress;

    
    private String deliveryName;

    
    private String deliveryPhone;

    
    private Integer deliveryStatus;

    
    private BigDecimal totalFee;

    
    private BigDecimal payFee;

    
    private BigDecimal discountFee;

    
    private BigDecimal deliveryFee;

    
    private BigDecimal postFee;

    
    private BigDecimal packageFee;

    
    private String channelSourceId;

    
    private String channelSourceName;

    
    private String channelOrderId;

    
    private String storeCode;

    
    private String longitude;

    
    private String latitude;

    
    private String remark;

    
    private Integer arriveType;

    
    private Long storeId;

    
    private String orderFrom;

    
    private Integer orderIndex;

    
    private BigDecimal estimatedIncome;

    
    private String subOrdersJson;

    
    private String discountsJson;

    
    private java.time.LocalDateTime etlTime;

    
    private String tenantId;

    
    private String creator;

    
    private String updater;

    
    private Long updateTime;

    
    private Boolean deleted;

    
    private java.math.BigDecimal settlementAmount;

    
    private Integer settlementStatus;

    
    private String regionCode;

    
    private java.time.LocalDateTime expectArriveTime;

    
    private java.time.LocalDateTime endTime;

    
    private java.time.LocalDate lastBillDate;

    
    private Long lastBillAmount;

    
    private Integer lastBillStatus;
}
