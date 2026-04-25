package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficApiRpsRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficHourlyStatsRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficTodayStatsRespVO;
import cn.iocoder.yudao.module.ele.service.traffic.EleTrafficMetricsCollector;
import cn.iocoder.yudao.module.ele.service.traffic.EleTrafficMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "管理后台 - 翱象流量监控")
@RestController
@RequestMapping("/ele/traffic")
@Validated
@TenantIgnore
public class EleTrafficMonitorController {

    @Resource
    private EleTrafficMonitorService trafficMonitorService;

    @GetMapping("/today-stats")
    @Operation(summary = "获取当日流量统计")
    public CommonResult<EleTrafficTodayStatsRespVO> getTodayStats() {
        return CommonResult.success(trafficMonitorService.getTodayStats());
    }

    @GetMapping("/stats")
    @Operation(summary = "获取指定日期流量统计")
    public CommonResult<EleTrafficTodayStatsRespVO> getStatsByDate(
            @Parameter(description = "日期格式: yyyyMMdd", required = true) @RequestParam String date) {
        return CommonResult.success(trafficMonitorService.getStatsByDate(date));
    }

    @GetMapping("/hourly-stats")
    @Operation(summary = "获取小时级流量统计")
    public CommonResult<List<EleTrafficHourlyStatsRespVO>> getHourlyStats() {
        return CommonResult.success(trafficMonitorService.getHourlyStats());
    }

    @GetMapping("/hourly-stats-by-date")
    @Operation(summary = "获取指定日期小时级流量统计")
    public CommonResult<List<EleTrafficHourlyStatsRespVO>> getHourlyStatsByDate(
            @Parameter(description = "日期格式: yyyyMMdd", required = true) @RequestParam String date) {
        return CommonResult.success(trafficMonitorService.getHourlyStatsByDate(date));
    }

    @GetMapping("/available-dates")
    @Operation(summary = "获取可用日期列表")
    public CommonResult<List<String>> getAvailableDates() {
        return CommonResult.success(trafficMonitorService.getAvailableDates());
    }

    @GetMapping("/record/{traceId}")
    @Operation(summary = "获取单条请求的流量信息")
    public CommonResult<EleTrafficMetricsCollector.RealtimeRecord> getSingleRecord(
            @Parameter(description = "请求Trace ID", required = true) @PathVariable String traceId) {
        return CommonResult.success(trafficMonitorService.getSingleRecord(traceId));
    }

    @PostMapping("/reset")
    @Operation(summary = "重置当日流量统计（手动触发）")
    public CommonResult<Boolean> resetStats() {
        trafficMonitorService.resetTodayStats();
        return CommonResult.success(true);
    }

    @GetMapping("/api-rps")
    @Operation(summary = "获取各接口实时RPS")
    public CommonResult<List<EleTrafficApiRpsRespVO>> getApiRps() {
        return CommonResult.success(trafficMonitorService.getApiRps());
    }
}
