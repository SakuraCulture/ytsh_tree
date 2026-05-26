package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;


@TableName("order_discount_table")
@Data
public class OrderDiscountDO {

    
    @TableId(type = IdType.AUTO)
    private Long discountId;

    
    private String orderId;

    
    private String subOrderId;

    
    private String activityId;

    
    private String activityName;

    
    private String activityOrderType;

    
    private String activityType;

    
    private String discountType;

    
    private BigDecimal discountFee;

    
    private BigDecimal merchantFee;

    
    private BigDecimal platformFee;

    
    private String creator;

    
    private Long createTime;

    
    private String updater;

    
    private Long updateTime;

    
    private Long tenantId;

    
    private Boolean deleted;
}