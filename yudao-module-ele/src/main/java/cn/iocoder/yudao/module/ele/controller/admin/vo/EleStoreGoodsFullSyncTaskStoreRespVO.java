package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店商品全量同步任务门店明细 Response VO")
@Data
public class EleStoreGoodsFullSyncTaskStoreRespVO {

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
    private LocalDateTime createTime;
}
