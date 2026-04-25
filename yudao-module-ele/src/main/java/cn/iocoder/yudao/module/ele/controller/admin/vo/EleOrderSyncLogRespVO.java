package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 翱象订单同步日志 Response VO")
@Data
public class EleOrderSyncLogRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "平台门店ID")
    private String platformStoreId;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "外部门店编码")
    private String erpStoreCode;

    @Schema(description = "本次同步起始时间(秒级时间戳)")
    private Long lastSyncTime;

    @Schema(description = "本次同步结束时间(秒级时间戳)")
    private Long syncTime;

    @Schema(description = "本次同步订单数量")
    private Integer syncCount;

    @Schema(description = "成功数")
    private Integer successCount;

    @Schema(description = "失败数")
    private Integer failCount;

    @Schema(description = "同步开始时间(秒级时间戳)")
    private Long syncStartTime;

    @Schema(description = "同步结束时间(秒级时间戳)")
    private Long syncEndTime;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "同步状态(0失败1成功)")
    private Integer status;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}