package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("ele_store_inventory_governance_pool")
@KeySequence("ele_store_inventory_governance_pool_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EleStoreInventoryGovernancePoolDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String skuCode;
    private String subSkuCode;
    private Long inventoryShadowId;
    private String reasonCode;
    private String reasonMsg;
    private String processStatus;
    private String rawPayload;
    private String remark;
    private Long uniqueDeleted;
}
