package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么订单状态日志 Response VO")
@Data
public class EleOrderStatusLogRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "平台类型")
    private String platformType;

    @Schema(description = "订单 ID")
    private String orderId;

    @Schema(description = "平台订单号")
    private String channelOrderId;

    @Schema(description = "门店 ID")
    private Long storeId;

    @Schema(description = "变更前订单状态")
    private String beforeOrderStatus;

    @Schema(description = "变更后订单状态")
    private String afterOrderStatus;

    @Schema(description = "变更前配送状态")
    private String beforeDeliveryStatus;

    @Schema(description = "变更后配送状态")
    private String afterDeliveryStatus;

    @Schema(description = "变更前平台状态")
    private String beforePlatformStatus;

    @Schema(description = "变更后平台状态")
    private String afterPlatformStatus;

    @Schema(description = "变更来源")
    private String changeSource;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "状态快照")
    private String snapshotContent;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
