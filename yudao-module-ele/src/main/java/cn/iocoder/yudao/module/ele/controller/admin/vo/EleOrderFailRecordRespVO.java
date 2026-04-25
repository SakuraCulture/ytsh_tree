package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么订单失败记录 Response VO")
@Data
public class EleOrderFailRecordRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "平台类型")
    private String platformType;

    @Schema(description = "门店 ID")
    private Long storeId;

    @Schema(description = "订单 ID")
    private String orderId;

    @Schema(description = "平台订单号")
    private String channelOrderId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "失败阶段")
    private String failStage;

    @Schema(description = "失败编码")
    private String failCode;

    @Schema(description = "失败信息")
    private String failMessage;

    @Schema(description = "请求参数快照")
    private String requestParam;

    @Schema(description = "响应内容快照")
    private String responseContent;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;

    @Schema(description = "处理状态")
    private String processStatus;

    @Schema(description = "任务 ID")
    private String taskId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "平台门店ID")
    private String platformStoreId;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "ERP门店编码")
    private String erpStoreCode;

    @Schema(description = "创建时间(秒级时间戳)")
    private Long createTime;

    @Schema(description = "更新时间(秒级时间戳)")
    private Long updateTime;

    @Schema(description = "创建时间格式化")
    public LocalDateTime getCreateTimeDateTime() {
        if (createTime != null) {
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(createTime),
                    java.time.ZoneId.systemDefault());
        }
        return null;
    }
}
