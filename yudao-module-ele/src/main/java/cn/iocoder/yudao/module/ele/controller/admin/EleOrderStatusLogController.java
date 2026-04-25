package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderStatusLogPageRespVO;
import cn.iocoder.yudao.module.ele.service.EleOrderStatusLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "管理后台 - 翱象订单状态变更日志")
@RestController
@RequestMapping("/ele/status-log")
@Validated
public class EleOrderStatusLogController {

    @Resource
    private EleOrderStatusLogService eleOrderStatusLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询状态变更日志")
    public CommonResult<PageResult<EleOrderStatusLogPageRespVO>> getStatusLogPage(
            @Parameter(description = "内部订单号") @RequestParam(required = false) String orderId,
            @Parameter(description = "平台订单号") @RequestParam(required = false) String channelOrderId,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "变更来源") @RequestParam(required = false) String changeSource,
            @Parameter(description = "起始时间(秒级时间戳)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(秒级时间戳)") @RequestParam(required = false) Long endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {

        PageResult<EleOrderStatusLogPageRespVO> result = eleOrderStatusLogService.getStatusLogPage(
                orderId, channelOrderId, storeId, changeSource, startTime, endTime, pageNo, pageSize);
        return CommonResult.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取状态变更日志详情")
    public CommonResult<EleOrderStatusLogPageRespVO> getStatusLog(
            @Parameter(description = "状态变更日志ID") @PathVariable Long id) {
        EleOrderStatusLogPageRespVO result = eleOrderStatusLogService.getStatusLogById(id);
        return CommonResult.success(result);
    }
}