package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("ele_order_sync_log")
@Data
public class EleOrderSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String syncBatchId;

    private String platformStoreId;

    private String merchantCode;

    private String erpStoreCode;

    private String storeName;

    private Long lastSyncTime;

    private LocalDateTime syncStartTime;

    private LocalDateTime syncEndTime;

    private Long syncTime;

    private Integer syncCount;

    private Integer totalPulled;

    private Integer successCount;

    private Integer failCount;

    private Integer failedOrderCount;

    private Integer status;

    private Integer partialFailed;

    private String failedOrderIds;

    private String syncMode;

    private Integer threadCount;

    private String checkpointInfo;

    private String errorMsg;

    private Long createTime;
}