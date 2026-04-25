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
@TableName("ele_store_goods_full_sync_task_store")
public class EleStoreGoodsFullSyncTaskStoreDO extends TenantBaseDO {

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
    private Integer currentPage;
    private Integer totalPage;
    private Integer pageSize;
    private Integer totalSkuCount;
    private Integer successCount;
    private Integer failCount;
    private Integer governanceCount;
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
