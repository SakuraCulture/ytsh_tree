package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("ele_store_inventory_shadow")
@KeySequence("ele_store_inventory_shadow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EleStoreInventoryShadowDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String skuCode;
    private String subSkuCode;
    private String matchedProductSkuId;
    private String matchedStoreProductId;
    private Integer availableForSale;
    private Integer reservedAmount;
    private Integer physicalStockTotalAmount;
    private Integer physicalStockAvailableAmount;
    private Integer physicalStockOccupiedAmount;
    private Integer physicalStockIntransitAmount;
    private String ownerCode;
    private String ownerName;
    private String matchStatus;
    private String reasonCode;
    private String reasonMsg;
    private String rawPayload;
    private LocalDateTime lastQueryTime;
    private Long uniqueDeleted;
}
