package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "管理后台 - 线程池报警阈值设置 Request VO")
@Data
public class ThreadPoolAlarmThresholdReqVO {

    @Schema(description = "线程池Bean名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "eleOrderSyncExecutor")
    private String poolName;

    @Schema(description = "队列使用率报警阈值(%)", requiredMode = Schema.RequiredMode.REQUIRED, example = "80")
    @Min(value = 1, message = "队列报警阈值最小为1")
    @Max(value = 100, message = "队列报警阈值最大为100")
    private Integer queueThresholdPercent;

    @Schema(description = "线程活跃率报警阈值(%)", requiredMode = Schema.RequiredMode.REQUIRED, example = "90")
    @Min(value = 1, message = "活跃率报警阈值最小为1")
    @Max(value = 100, message = "活跃率报警阈值最大为100")
    private Integer activeThresholdPercent;

    @Schema(description = "是否启用报警", example = "true")
    private Boolean enabled;
}
