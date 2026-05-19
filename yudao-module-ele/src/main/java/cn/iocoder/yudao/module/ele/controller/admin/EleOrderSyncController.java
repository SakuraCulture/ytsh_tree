package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderSyncLog;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderSyncLogMapper;
import cn.iocoder.yudao.module.ele.service.ShutdownStateManager;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor.SyncResult;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 饿了么订单同步管理接口
 *
 * 提供全部门店并行同步、同步状态查询等接口。
 *
 * @author 优团科技数字化团队
 */
@Slf4j
@Tag(name = "管理后台 - 饿了么订单同步")
@RestController
@RequestMapping("/ele/order-sync")
@Validated
@TenantIgnore
public class EleOrderSyncController {

    @Resource
    private StoreService storeService;

    @Resource
    private EleOrderSyncTaskExecutor syncTaskExecutor;

    @Resource
    private ShutdownStateManager shutdownStateManager;

    @Resource
    private EleOrderSyncLogMapper eleOrderSyncLogMapper;

    @PostMapping("/sync-all")
    @Operation(summary = "手动触发全部门店并行订单同步（支持指定门店和时间范围）")
    public CommonResult<Map<String, Object>> syncAllStores(
            @Parameter(description = "起始时间（秒级时间戳）") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间（秒级时间戳）") @RequestParam(required = false) Long endTime,
            @Parameter(description = "平台门店ID（可选，传入则仅同步指定门店）") @RequestParam(required = false) String platformStoreId) {
        List<StorePlatformRespVO> stores;
        
        if (platformStoreId != null && !platformStoreId.isEmpty()) {
            StorePlatformRespVO store = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
            if (store == null) {
                return CommonResult.error(400, "门店不存在: " + platformStoreId);
            }
            stores = List.of(store);
            log.info("【单店同步】指定门店: platformStoreId={}", platformStoreId);
        } else {
            stores = storeService.getAllPlatformStoresByPlatformCode(null);
            log.info("【全店同步】共{}家门店", stores.size());
        }
        
        if (stores == null || stores.isEmpty()) {
            return CommonResult.success(Map.of(
                    "message", "暂无需要同步的门店",
                    "totalCount", 0,
                    "successCount", 0,
                    "failCount", 0
            ));
        }

        if (!shutdownStateManager.startBatchSync(stores.size())) {
            return CommonResult.error(409, "已有订单同步任务正在执行，请稍后再试");
        }

        boolean success = false;
        try {
            SyncResult result = syncTaskExecutor.executeSync(stores, startTime, endTime);
            success = result.isCompleted() && result.getFailCount() == 0;

            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("elapsedSeconds", result.getElapsedSeconds());
            response.put("completed", result.isCompleted());
            response.put("failedStores", result.getFailedStores());
            return CommonResult.success(response);
        } catch (Exception e) {
            log.error("【手动同步】订单同步执行异常，startTime={}, endTime={}, platformStoreId={}", startTime, endTime, platformStoreId, e);
            throw e;
        } finally {
            shutdownStateManager.finishBatchSync(success);
        }
    }

    @GetMapping("/error-detail")
    @Operation(summary = "查询同步错误详情")
    public CommonResult<Map<String, Object>> getErrorDetail(
            @Parameter(description = "同步日志ID", required = true) @RequestParam Long syncLogId) {
        EleOrderSyncLog syncLog = eleOrderSyncLogMapper.selectById(syncLogId);
        if (syncLog == null) {
            return CommonResult.error(404, "同步日志不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("syncLogId", syncLogId);
        result.put("storeName", syncLog.getStoreName());
        result.put("syncStartTime", syncLog.getSyncStartTime());

        Map<String, Object> pullError = new HashMap<>();
        pullError.put("code", syncLog.getPullErrorCode());
        pullError.put("detail", syncLog.getPullErrorDetail() != null ?
                JSONUtil.parse(syncLog.getPullErrorDetail()) : null);
        result.put("pullError", pullError);

        Map<String, Object> saveError = new HashMap<>();
        saveError.put("code", syncLog.getSaveErrorCode());
        saveError.put("detail", syncLog.getSaveErrorDetail() != null ?
                JSONUtil.parse(syncLog.getSaveErrorDetail()) : null);
        result.put("saveError", saveError);

        Map<String, Object> reconError = new HashMap<>();
        reconError.put("code", syncLog.getReconciliationErrorCode());
        reconError.put("detail", syncLog.getReconciliationErrorDetail() != null ?
                JSONUtil.parse(syncLog.getReconciliationErrorDetail()) : null);
        result.put("reconciliationError", reconError);

        // 对账数据
        Map<String, Object> reconciliation = new HashMap<>();
        reconciliation.put("expectedTotal", syncLog.getExpectedTotal());
        reconciliation.put("actualTotal", syncLog.getActualTotal());
        reconciliation.put("savedTotal", syncLog.getSavedTotal());
        reconciliation.put("discrepancyRate", syncLog.getDiscrepancyRate());
        reconciliation.put("dataIntegrity", syncLog.getDataIntegrity());
        reconciliation.put("retryCount", syncLog.getRetryCount());
        reconciliation.put("apiStatusCounts", syncLog.getApiStatusCounts() != null ?
                JSONUtil.parse(syncLog.getApiStatusCounts()) : null);
        reconciliation.put("savedStatusCounts", syncLog.getSavedStatusCounts() != null ?
                JSONUtil.parse(syncLog.getSavedStatusCounts()) : null);
        result.put("reconciliation", reconciliation);

        return CommonResult.success(result);
    }

}
