package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 翱象订单同步统计 Response VO")
@Data
public class EleOrderSyncStatsRespVO {

    @Schema(description = "平台门店ID")
    private String platformStoreId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "同步总次数")
    private Integer totalSyncCount;

    @Schema(description = "成功次数")
    private Integer successCount;

    @Schema(description = "失败次数")
    private Integer failCount;

    @Schema(description = "成功率(%)")
    private Double successRate;

    @Schema(description = "平均耗时(秒)")
    private Double avgDuration;

    @Schema(description = "最近同步时间(秒级时间戳)")
    private Long lastSyncTime;
}
