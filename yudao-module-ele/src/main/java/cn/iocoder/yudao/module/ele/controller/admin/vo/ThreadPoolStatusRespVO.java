package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 线程池状态 Response VO")
@Data
public class ThreadPoolStatusRespVO {

    @Schema(description = "线程池Bean名称")
    private String poolName;

    @Schema(description = "线程名前缀")
    private String threadNamePrefix;

    @Schema(description = "核心线程数")
    private Integer corePoolSize;

    @Schema(description = "最大线程数")
    private Integer maxPoolSize;

    @Schema(description = "当前线程总数")
    private Integer poolSize;

    @Schema(description = "活跃线程数")
    private Integer activeCount;

    @Schema(description = "线程活跃率(%)")
    private Double activePercent;

    @Schema(description = "队列中等待的任务数")
    private Integer queueSize;

    @Schema(description = "队列总容量")
    private Integer queueCapacity;

    @Schema(description = "队列使用率(%)")
    private Double queueUsagePercent;

    @Schema(description = "已完成任务总数")
    private Long completedTaskCount;

    @Schema(description = "总任务数")
    private Long taskCount;

    @Schema(description = "拒绝策略")
    private String rejectedPolicy;

    @Schema(description = "健康状态: HEALTHY/WARNING/CRITICAL")
    private String healthStatus;

    @Schema(description = "状态描述")
    private String healthMessage;
}
