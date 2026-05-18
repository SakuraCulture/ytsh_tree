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
@TableName("ele_store_inventory_batch_task_store")
public class EleStoreInventoryBatchTaskStoreDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private String taskNo;
    private String storeId;
    private String storeName;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String status;
    private Integer currentBatchNo;
    private Integer totalBatchNo;
    private Integer totalSkuCount;
    private Integer formalSuccessCount;
    private Integer shadowSuccessCount;
    private Integer governanceCount;
    private Integer failureCount;
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
