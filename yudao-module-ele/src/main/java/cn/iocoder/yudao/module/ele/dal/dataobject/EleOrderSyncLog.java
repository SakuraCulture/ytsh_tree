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

    private String syncMode;

    private Integer threadCount;

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

    // ============== 新增: 对账相关字段 ==============
    private Integer expectedTotal;

    private Integer actualTotal;

    private Integer savedTotal;

    private java.math.BigDecimal discrepancyRate;

    private Integer dataIntegrity;

    private Integer retryCount;

    private String apiStatusCounts;

    private String savedStatusCounts;

    private String pageCounts;

    // ============== 新增: 错误信息字段 ==============
    private String pullErrorCode;

    private String pullErrorDetail;

    private String saveErrorCode;

    private String saveErrorDetail;

    private String reconciliationErrorCode;

    private String reconciliationErrorDetail;

    private Integer pauseSync;

    private String compensationInfo;

    // ============== 原有字段 ==============
    private Integer successCount;

    private Integer failCount;

    private Integer failedOrderCount;

    private Integer status;

    private Integer partialFailed;

    private String failedOrderIds;

    private String checkpointInfo;

    private String errorMsg;

    private Long createTime;
}
