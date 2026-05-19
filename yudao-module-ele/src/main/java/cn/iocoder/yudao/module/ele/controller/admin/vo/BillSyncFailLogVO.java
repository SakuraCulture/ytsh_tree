package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 账单同步失败日志 Response VO")
@Data
public class BillSyncFailLogVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "账单日期")
    private String billDate;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "失败页码")
    private Integer failPage;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "重试状态文本")
    private String retryStatusText;

    @Schema(description = "重试状态: 0-待重试 1-重试中 2-已解决")
    private Integer retryStatus;

    @Schema(description = "最后重试时间")
    private String lastRetryTime;

    @Schema(description = "首次同步时间")
    private String syncTime;
}
