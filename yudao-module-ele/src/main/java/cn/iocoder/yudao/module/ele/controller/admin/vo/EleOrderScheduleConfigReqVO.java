package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 订单定时同步配置 Request VO")
@Data
public class EleOrderScheduleConfigReqVO {

    @Schema(description = "是否开启", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    @Schema(description = "定时模式: time/dayOfMonth/weekDay", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scheduleType;

    @Schema(description = "Cron表达式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cronExpression;

    @Schema(description = "执行时间点列表 (HH:mm:ss)")
    private List<String> timePoints;

    @Schema(description = "每月执行天数")
    private List<Integer> daysOfMonth;

    @Schema(description = "按天数模式执行时间 (HH:mm:ss)")
    private String dayOfMonthTime;

    @Schema(description = "每周执行星期")
    private List<Integer> weekDays;

    @Schema(description = "按周模式执行时间 (HH:mm:ss)")
    private String weekDayTime;

    @Schema(description = "间隔模式开始时间 (HH:mm:ss)")
    private String intervalStartTime;

    @Schema(description = "间隔时间（小时）")
    private Integer intervalHours;
}
