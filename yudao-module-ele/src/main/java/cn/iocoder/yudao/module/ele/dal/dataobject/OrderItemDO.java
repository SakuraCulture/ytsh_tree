package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;


@TableName("order_item_table")
@Data
public class OrderItemDO {

    
    @TableId(type = IdType.AUTO)
    private Long itemId;

    
    private String orderId;

    
    private String subOrderId;

    
    private String skuCode;

    
    private String subSkuCode;

    
    private Long productSkuId;

    
    private String skuName;

    
    private String barcode;

    
    private String specification;

    
    private BigDecimal weight;

    
    private BigDecimal totalWeight;

    
    private Integer buyAmount;

    
    private BigDecimal price;

    
    private BigDecimal totalFee;

    
    private BigDecimal payFee;

    
    private Integer productType;

    
    private String goodsType;

    
    private Integer num;

    
    private String cabinetCode;

    
    private Boolean exchangeFlag;

    
    private BigDecimal exchangeAmount;

    
    private Boolean giftFlag;

    
    private Boolean outboundFlag;

    
    private String erpStoreCode;

    
    private Long tenantId;

    
    private java.time.LocalDateTime etlTime;

    
    private String creator;

    
    private Long createTime;

    
    private String updater;

    
    private Long updateTime;

    
    private Boolean deleted;
}
