package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.BillSyncFailLogVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.OrderBillVO;
import cn.iocoder.yudao.module.ele.service.EleBillSyncService;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 账单同步管理")
@RestController
@RequestMapping("/ele/bill-sync")
public class EleBillSyncController {

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private EleBillSyncService eleBillSyncService;

    @GetMapping("/fail-log-list")
    @Operation(summary = "查询账单同步失败日志")
    @PreAuthorize("@ss.hasPermission('ele:bill:sync-log')")
    public CommonResult<List<BillSyncFailLogVO>> getFailLogList() {
        return success(eleBillSyncService.getFailLogList());
    }

    @GetMapping("/bill-info")
    @Operation(summary = "查询订单账单详情")
    @Parameter(name = "orderId", description = "订单号", required = true, example = "123456")
    @PreAuthorize("@ss.hasPermission('ele:bill:query')")
    public CommonResult<OrderBillVO> getBillInfo(@RequestParam("orderId") String orderId) {
        return success(eleBillSyncService.getOrderBillInfo(orderId));
    }

    @GetMapping("/bill-summary")
    @Operation(summary = "查询订单账单汇总(正向单+代运营业务)")
    @Parameter(name = "orderId", description = "订单号", required = true, example = "123456")
    @PreAuthorize("@ss.hasPermission('ele:bill:query')")
    public CommonResult<OrderBillVO> getBillSummary(@RequestParam("orderId") String orderId) {
        return success(eleBillSyncService.getBillSummary(orderId));
    }

    @PostMapping("/sync-by-date")
    @Operation(summary = "按日期同步账单")
    @Parameter(name = "billDate", description = "账单日期(yyyy-MM-dd)", required = true, example = "2026-05-09")
    @PreAuthorize("@ss.hasPermission('ele:bill:sync')")
    public CommonResult<String> syncByDate(@RequestParam("billDate") String billDate) {
        eleBillSyncService.syncAllBillsByDate(billDate);
        return success("账单同步任务已启动");
    }

    @PostMapping("/sync-by-date-range")
    @Operation(summary = "按日期范围同步账单")
    @Parameter(name = "startDate", description = "开始日期(yyyy-MM-dd)", required = true, example = "2026-05-01")
    @Parameter(name = "endDate", description = "结束日期(yyyy-MM-dd)", required = true, example = "2026-05-31")
    @PreAuthorize("@ss.hasPermission('ele:bill:sync')")
    public CommonResult<String> syncByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        eleBillSyncService.syncAllBillsByDateRange(startDate, endDate);
        return success("账单范围同步任务已启动");
    }

    @PostMapping("/retry-by-store")
    @Operation(summary = "按门店重试账单同步")
    @PreAuthorize("@ss.hasPermission('ele:bill:retry')")
    public CommonResult<String> retryByStore(
            @RequestParam("merchantCode") String merchantCode,
            @RequestParam("storeCode") String storeCode,
            @RequestParam("billDate") String billDate) {
        eleBillSyncService.syncBillByStoreAndDate(merchantCode, storeCode, billDate);
        return success("门店账单重试任务已启动");
    }

    @PostMapping("/retry-by-order")
    @Operation(summary = "按订单重试账单同步")
    @Parameter(name = "orderId", description = "订单号", required = true, example = "123456")
    @PreAuthorize("@ss.hasPermission('ele:bill:retry')")
    public CommonResult<String> retryByOrder(@RequestParam("orderId") String orderId) {
        eleBillSyncService.retryByOrderId(orderId);
        return success("订单账单重试任务已启动");
    }

    @PostMapping("/retry-all")
    @Operation(summary = "批量重试所有待重试记录")
    @PreAuthorize("@ss.hasPermission('ele:bill:retry')")
    public CommonResult<String> retryAllPending() {
        eleBillSyncService.retryFailedBillSync();
        return success("失败账单重试任务已启动");
    }
}
