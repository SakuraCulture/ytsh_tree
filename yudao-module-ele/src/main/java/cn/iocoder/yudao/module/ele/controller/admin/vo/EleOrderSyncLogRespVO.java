package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 翱象订单同步日志 Response VO")
@Data
public class EleOrderSyncLogRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "同步批次ID")
    private String syncBatchId;

    @Schema(description = "同步模式(SINGLE/MULTI)")
    private String syncMode;

    @Schema(description = "线程数")
    private Integer threadCount;

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

    @Schema(description = "API拉取订单总数")
    private Integer totalPulled;

    @Schema(description = "预期订单总数(API返回)")
    private Integer expectedTotal;

    @Schema(description = "实际拉取订单总数")
    private Integer actualTotal;

    @Schema(description = "实际落库订单总数")
    private Integer savedTotal;

    @Schema(description = "差异率(%)")
    private BigDecimal discrepancyRate;

    @Schema(description = "数据完整性(1完整/2部分/3严重)")
    private Integer dataIntegrity;

    @Schema(description = "补偿重试次数")
    private Integer retryCount;

    @Schema(description = "API各状态数量JSON")
    private String apiStatusCounts;

    @Schema(description = "落库各状态数量JSON")
    private String savedStatusCounts;

    @Schema(description = "各状态分页数量JSON")
    private String pageCounts;

    @Schema(description = "拉取错误代码")
    private String pullErrorCode;

    @Schema(description = "拉取错误详情JSON")
    private String pullErrorDetail;

    @Schema(description = "落库错误代码")
    private String saveErrorCode;

    @Schema(description = "落库错误详情JSON")
    private String saveErrorDetail;

    @Schema(description = "对账错误代码")
    private String reconciliationErrorCode;

    @Schema(description = "对账错误详情JSON")
    private String reconciliationErrorDetail;

    @Schema(description = "是否暂停同步(0否1是)")
    private Integer pauseSync;

    @Schema(description = "补偿信息JSON")
    private String compensationInfo;

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