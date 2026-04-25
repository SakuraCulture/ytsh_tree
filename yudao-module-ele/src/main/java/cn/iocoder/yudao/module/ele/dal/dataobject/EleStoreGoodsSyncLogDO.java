package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ele_store_goods_sync_log")
public class EleStoreGoodsSyncLogDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private String ticket;

    private String apiCode;

    private String apiName;

    private String merchantCode;

    private String erpStoreCode;

    private Long platformId;

    private String storeId;

    private String platformStoreId;

    private String skuCode;

    private String subSkuCode;

    private String operationType;

    private Integer pageNo;

    private Integer pageSize;

    private Integer dataCount;

    private Boolean success;

    private String resultCode;

    private String resultMsg;

    private Integer durationMs;

    private String requestBody;

    private String responseBody;
}
