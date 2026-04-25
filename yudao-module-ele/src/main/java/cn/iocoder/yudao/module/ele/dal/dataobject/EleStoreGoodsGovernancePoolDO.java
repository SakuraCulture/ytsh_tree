package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ele_store_goods_governance_pool")
public class EleStoreGoodsGovernancePoolDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantCode;

    private String erpStoreCode;

    private Long platformId;

    private String storeId;

    private String platformStoreId;

    private String skuCode;

    private String subSkuCode;

    private String spuCode;

    private String goodsLevel;

    private String operationType;

    private String reasonCode;

    private String reasonMsg;

    private String processStatus;

    private String rawPayload;

    private String remark;
}
