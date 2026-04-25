package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.module.ele.service.dto.EleSyncSubmitRespDTO;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor.SyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 饿了么订单同步管理接口
 *
 * 提供全部门店并行同步、同步状态查询等接口。
 *
 * @author 优团科技数字化团队
 */
@Tag(name = "管理后台 - 饿了么订单同步")
@RestController
@RequestMapping("/ele/order-sync")
@Validated
@TenantIgnore
public class EleOrderSyncController {

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private StoreService storeService;

    @Resource
    private EleOrderSyncTaskExecutor syncTaskExecutor;

    @PostMapping("/sync-all")
    @Operation(summary = "手动触发全部门店并行订单同步")
    public CommonResult<Map<String, Object>> syncAllStores() {
        List<StorePlatformRespVO> stores = storeService.getOpenPlatformStoresByPlatformCode(null);
        if (stores == null || stores.isEmpty()) {
            return CommonResult.success(Map.of(
                    "message", "暂无需要同步的门店",
                    "totalCount", 0,
                    "successCount", 0,
                    "failCount", 0
            ));
        }

        SyncResult result = syncTaskExecutor.executeSync(stores, null, null);

        Map<String, Object> response = new HashMap<>();
        response.put("totalCount", result.getTotalCount());
        response.put("successCount", result.getSuccessCount());
        response.put("failCount", result.getFailCount());
        response.put("elapsedSeconds", result.getElapsedSeconds());
        response.put("completed", result.isCompleted());
        response.put("failedStores", result.getFailedStores());
        return CommonResult.success(response);
    }

    @PostMapping("/sync-range")
    @Operation(summary = "按时间范围触发全部门店并行订单同步")
    public CommonResult<Map<String, Object>> syncAllStoresByRange(
            @Parameter(description = "起始时间（秒级时间戳）", required = true) @RequestParam Long startTime,
            @Parameter(description = "结束时间（秒级时间戳）", required = true) @RequestParam Long endTime) {
        List<StorePlatformRespVO> stores = storeService.getOpenPlatformStoresByPlatformCode(null);
        if (stores == null || stores.isEmpty()) {
            return CommonResult.success(Map.of(
                    "message", "暂无需要同步的门店",
                    "totalCount", 0,
                    "successCount", 0,
                    "failCount", 0
            ));
        }

        SyncResult result = syncTaskExecutor.executeSync(stores, startTime, endTime);

        Map<String, Object> response = new HashMap<>();
        response.put("totalCount", result.getTotalCount());
        response.put("successCount", result.getSuccessCount());
        response.put("failCount", result.getFailCount());
        response.put("elapsedSeconds", result.getElapsedSeconds());
        response.put("completed", result.isCompleted());
        response.put("failedStores", result.getFailedStores());
        return CommonResult.success(response);
    }

    @PostMapping("/sync/submit")
    @Operation(summary = "提交同步/补偿任务（异步）")
    public CommonResult<EleSyncSubmitRespDTO> submitSyncTask(
            @Parameter(description = "平台门店ID") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "saas商家编码") @RequestParam(required = false) String merchantCode,
            @Parameter(description = "外部门店编码") @RequestParam(required = false) String erpStoreCode,
            @Parameter(description = "起始时间") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Long endTime,
            @Parameter(description = "是否补偿") @RequestParam(defaultValue = "false") boolean compensate) {
        return CommonResult.success(eleOrderService.submitSyncTask(platformStoreId, merchantCode, erpStoreCode, startTime, endTime, compensate));
    }

}
