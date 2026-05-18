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
@TableName("ele_store_inventory_batch_task")
public class EleStoreInventoryBatchTaskDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private String sourceType;
    private String scope;
    private String status;
    private Integer totalStoreCount;
    private Integer finishedStoreCount;
    private Integer totalBatchCount;
    private Integer finishedBatchCount;
    private Integer totalSkuCount;
    private Integer formalSuccessCount;
    private Integer shadowSuccessCount;
    private Integer governanceCount;
    private Integer failureCount;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
