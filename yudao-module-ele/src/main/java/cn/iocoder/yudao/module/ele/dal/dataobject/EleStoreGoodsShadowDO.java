package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ele_store_goods_shadow")
@KeySequence("ele_store_goods_shadow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EleStoreGoodsShadowDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String spuCode;
    private String skuCode;
    private String subSkuCode;
    private String title;
    private String mainPic;
    private String subPics;
    private String frontCategory;
    private String brandName;
    private String specification;
    private BigDecimal salePrice;
    private String posStatus;
    private Integer isActive;
    private String rawPayload;
    private String matchStatus;
    private String matchedProductSkuId;
    private String mergedStoreProductId;
    private LocalDateTime lastSyncTime;
    private LocalDateTime matchedTime;
    private LocalDateTime mergedTime;
    private String conflictReason;
    private Integer goodsSource;
    private Long uniqueDeleted;
}
