package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店库存批量任务 Response VO")
@Data
public class EleStoreInventoryBatchTaskRespVO {

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
    private LocalDateTime createTime;
}
