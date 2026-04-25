package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店商品全量同步任务 Response VO")
@Data
public class EleStoreGoodsFullSyncTaskRespVO {

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
    private LocalDateTime createTime;
}
