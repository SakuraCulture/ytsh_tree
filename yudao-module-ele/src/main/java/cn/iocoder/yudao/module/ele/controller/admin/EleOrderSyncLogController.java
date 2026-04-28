package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderSyncLogRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderSyncStatsRespVO;
import cn.iocoder.yudao.module.ele.service.EleOrderSyncLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "管理后台 - 翱象订单同步日志")
@RestController
@RequestMapping("/ele/sync-log")
@Validated
public class EleOrderSyncLogController {

    @Resource
    private EleOrderSyncLogService eleOrderSyncLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询同步日志")
    @PermitAll
    public CommonResult<PageResult<EleOrderSyncLogRespVO>> getSyncLogPage(
            @Parameter(description = "平台门店ID") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "外部门店编码") @RequestParam(required = false) String erpStoreCode,
            @Parameter(description = "同步状态(0失败1成功)") @RequestParam(required = false) Integer status,
            @Parameter(description = "起始时间(秒级时间戳)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(秒级时间戳)") @RequestParam(required = false) Long endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {

        PageResult<EleOrderSyncLogRespVO> result = eleOrderSyncLogService.getSyncLogPage(
                platformStoreId, erpStoreCode, status, startTime, endTime, pageNo, pageSize);
        return CommonResult.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取同步日志详情")
    @PermitAll
    public CommonResult<EleOrderSyncLogRespVO> getSyncLog(
            @Parameter(description = "同步日志ID") @PathVariable Long id) {
        EleOrderSyncLogRespVO result = eleOrderSyncLogService.getSyncLogById(id);
        return CommonResult.success(result);
    }

    @GetMapping("/stats/{platformStoreId}")
    @Operation(summary = "获取门店累计同步统计")
    @PermitAll
    public CommonResult<EleOrderSyncStatsRespVO> getStoreSyncStats(
            @Parameter(description = "平台门店ID", required = true) @PathVariable String platformStoreId) {
        EleOrderSyncStatsRespVO result = eleOrderSyncLogService.getStoreSyncStats(platformStoreId);
        return CommonResult.success(result);
    }
}