package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店库存批量任务门店明细 Response VO")
@Data
public class EleStoreInventoryBatchTaskStoreRespVO {

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
    private LocalDateTime createTime;
}
