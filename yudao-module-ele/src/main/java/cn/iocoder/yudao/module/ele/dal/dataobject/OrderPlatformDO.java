package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;


@TableName("order_platform_table")
@Data
public class OrderPlatformDO {

    
    private String orderId;

    
    private String platformType;

    
    private Integer deliveryPlatform;

    
    private Integer deliveryType;

    
    private BigDecimal platformCommissionFee;

    
    private String platformOrderStatus;

    
    private String platformDeliveryStatus;

    
    private String platformExtend;

    
    private java.time.LocalDateTime etlTime;

    
    private Long tenantId;

    
    private String creator;

    
    private Long createTime;

    
    private String updater;

    
    private Long updateTime;

    
    private Boolean deleted;
}
