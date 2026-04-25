package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ele_store_goods_full_sync_task")
public class EleStoreGoodsFullSyncTaskDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private String scope;
    private String merchantCode;
    private String erpStoreCode;
    private Boolean testMode;
    private String status;
    private Integer totalStoreCount;
    private Integer finishedStoreCount;
    private Integer totalPageCount;
    private Integer finishedPageCount;
    private Integer totalSkuCount;
    private Integer successCount;
    private Integer failCount;
    private Integer governanceCount;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
