package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.config.EleOrderSchedulerProperties;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderFailRecordRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderScheduleConfigReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderStatusLogRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.OrderBillVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.OrderPushSettingVO;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderMapper;
import cn.iocoder.yudao.module.ele.dal.redis.OrderSyncProgressCache;
import cn.iocoder.yudao.module.ele.exception.EleOrderSyncException;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.module.ele.service.EleBillSyncService;
import cn.iocoder.yudao.module.ele.service.EleApiRateLimiter;
import cn.iocoder.yudao.module.ele.service.ShutdownStateManager;
import cn.iocoder.yudao.module.ele.service.dto.StoreSyncProgress;
import cn.iocoder.yudao.module.ele.service.dto.EleCompensateProgressDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 翱象订单管理接口
 * 1. 订单列表查询（本地数据库）
 * 2. 订单列表查询（远程API）
 * 3. 订单详情查询
 * 4. 订单状态日志查询
 * 5. 订单补偿进度查询
 * 6. 订单补偿提交
 * 7. 订单失败记录查询
 * 8. 订单补偿提交
 * 
 * @description 订单补偿提交接口
 * @return 订单补偿提交结果
 * @author SMK
 * @date 2023-08-10
 * 
 */

@Tag(name = "管理后台 - 翱象订单")
@RestController
@RequestMapping("/ele/order")
@Validated
@TenantIgnore
public class EleOrderController {

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private EleBillSyncService eleBillSyncService;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ShutdownStateManager shutdownStateManager;

    @Resource
    private OrderSyncProgressCache orderSyncProgressCache;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private EleApiRateLimiter eleApiRateLimiter;

    @Resource
    private EleOrderSchedulerProperties eleOrderSchedulerProperties;

    @GetMapping("/list")
    @Operation(summary = "翱象订单列表查询（本地数据库）")
    @PermitAll
    public CommonResult<PageResult<OrderListRespDTO.OrderDetail>> getOrderList(
            @Parameter(description = "平台门店ID") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "门店ID") @RequestParam(required = false) String storeId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "起始时间(unix秒)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(unix秒)") @RequestParam(required = false) Long endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "订单ID/订单小号") @RequestParam(required = false) String orderId,
            @Parameter(description = "渠道订单号") @RequestParam(required = false) String channelOrderId,
            @Parameter(description = "收货人") @RequestParam(required = false) String buyerName,
            @Parameter(description = "手机号后缀") @RequestParam(required = false) String buyerPhoneSuffix,
            @Parameter(description = "商品名称") @RequestParam(required = false) String skuName,
            @Parameter(description = "渠道类型") @RequestParam(required = false) String channelType,
            @Parameter(description = "订单类型/到达类型") @RequestParam(required = false) Integer arriveType,
            @Parameter(description = "订单异常 normal/exception") @RequestParam(required = false) String exceptionType,
            @Parameter(description = "配送类型 1平台配送 2自配送 3自提") @RequestParam(required = false) Integer deliveryMode,
            @Parameter(description = "收货地址") @RequestParam(required = false) String address,
            @Parameter(description = "排序") @RequestParam(required = false) String orderSort) {

        PageResult<OrderListRespDTO.OrderDetail> result = eleOrderService.getOrdersFromLocal(
                platformStoreId, storeId, status, startTime, endTime, pageNo, pageSize,
                orderId, channelOrderId, buyerName, buyerPhoneSuffix, skuName, channelType, arriveType,
                exceptionType, deliveryMode, address, orderSort);
        return CommonResult.success(result);
    }

    @GetMapping("/status-counts")
    @Operation(summary = "按订单状态分组统计数量")
    @PermitAll
    public CommonResult<Map<Integer, Long>> getOrderStatusCounts(
            @Parameter(description = "平台门店ID") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "起始时间(unix秒)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(unix秒)") @RequestParam(required = false) Long endTime,
            @Parameter(description = "订单ID/订单小号") @RequestParam(required = false) String orderId,
            @Parameter(description = "渠道订单号") @RequestParam(required = false) String channelOrderId,
            @Parameter(description = "收货人") @RequestParam(required = false) String buyerName,
            @Parameter(description = "手机号后缀") @RequestParam(required = false) String buyerPhoneSuffix,
            @Parameter(description = "商品名称") @RequestParam(required = false) String skuName,
            @Parameter(description = "渠道类型") @RequestParam(required = false) String channelType,
            @Parameter(description = "订单类型/到达类型") @RequestParam(required = false) Integer arriveType,
            @Parameter(description = "订单异常 normal/exception") @RequestParam(required = false) String exceptionType,
            @Parameter(description = "配送类型 1平台配送 2自配送 3自提") @RequestParam(required = false) Integer deliveryMode,
            @Parameter(description = "收货地址") @RequestParam(required = false) String address) {

        Map<Integer, Long> counts = eleOrderService.getStatusCounts(platformStoreId, startTime, endTime,
                orderId, channelOrderId, buyerName, buyerPhoneSuffix, skuName, channelType, arriveType, exceptionType, deliveryMode, address);
        return CommonResult.success(counts);
    }

    @GetMapping("/list/remote")
    @Operation(summary = "翱象订单列表查询（远程API）")
    @PermitAll
    public CommonResult<OrderListRespDTO> getOrderListRemote(
            @Parameter(description = "平台门店ID") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "saas商家编码") @RequestParam(required = false) String merchantCode,
            @Parameter(description = "外部门店编码") @RequestParam(required = false) String erpStoreCode,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "起始时间(unix秒)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(unix秒)") @RequestParam(required = false) Long endTime) {

        List<OrderListRespDTO.OrderDetail> allOrders = new ArrayList<>();
        Set<String> fetchedScrollIds = new HashSet<>();
        String scrollId = null;
        long upstreamTotal = 0L;
        int pageNo = 1;
        final int pageSize = 100;

        while (true) {
            OrderListReqDTO req = new OrderListReqDTO();
            req.setPlatformStoreId(platformStoreId);
            req.setMerchantCode(merchantCode);
            req.setErpStoreCode(erpStoreCode);
            req.setStatus(status);
            req.setStartTime(startTime);
            req.setEndTime(endTime);
            req.setPageSize(pageSize);
            req.setScrollId(scrollId);

            OrderListRespDTO pageResult = eleOrderService.getOrderList(req);
            if (pageResult == null) {
                break;
            }

            if (pageNo == 1) {
                upstreamTotal = pageResult.getTotal() == null ? 0L : pageResult.getTotal();
            }

            List<OrderListRespDTO.OrderDetail> currentList = pageResult.getOrderList();
            if (currentList != null && !currentList.isEmpty()) {
                allOrders.addAll(currentList);
            }

            String nextScrollId = pageResult.getScrollId();

            if (nextScrollId == null || nextScrollId.isEmpty() || fetchedScrollIds.contains(nextScrollId)) {
                break;
            }

            fetchedScrollIds.add(nextScrollId);
            scrollId = nextScrollId;
            pageNo++;
        }

        OrderListRespDTO result = new OrderListRespDTO();
        result.setTotal((long) allOrders.size());
        result.setScrollId(null);
        result.setOrderList(allOrders);

        return CommonResult.success(result);
    }

    @GetMapping("/detail")
    @Operation(summary = "翱象订单详情查询（本地+远程）")
    @PermitAll
    public CommonResult<OrderDetailRespDTO> getOrderDetail(
            @Parameter(description = "平台门店ID") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "saas商家编码") @RequestParam(required = false) String merchantCode,
            @Parameter(description = "外部门店编码") @RequestParam(required = false) String erpStoreCode,
            @Parameter(description = "订单ID", required = true) @RequestParam String orderId) {

        OrderDetailRespDTO result = eleOrderService.getOrderDetail(platformStoreId, merchantCode, erpStoreCode,
                orderId);
        return CommonResult.success(result);
    }

    @GetMapping("/rate-limit/status")
    @Operation(summary = "查询翱象接口全局限流状态")
    @PermitAll
    public CommonResult<java.util.Map<String, Object>> getApiRateLimitStatus() {
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        int waitingCount = eleApiRateLimiter.getLocalWaitingCount();
        java.util.List<EleApiRateLimiter.ApiRateLimitStatus> apiStatuses = eleApiRateLimiter.getApiStatuses();
        status.put("globalQps", eleApiRateLimiter.getGlobalQps());
        status.put("hasBacklog", eleApiRateLimiter.hasBacklog());
        status.put("waitingCount", waitingCount);
        status.put("localWaitingCount", waitingCount);
        status.put("queueAlert", waitingCount > 0);
        status.put("apis", apiStatuses);
        status.put("message", waitingCount > 0
                ? "翱象接口请求已触发接口级限流，后续请求正在暂停排队"
                : "翱象接口限流状态正常");
        status.put("timestamp", System.currentTimeMillis());
        return CommonResult.success(status);
    }

    @GetMapping("/sync/status")
    @Operation(summary = "查询当前同步任务状态")
    @PermitAll
    public CommonResult<java.util.Map<String, Object>> getSyncStatus() {
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("isSyncing", shutdownStateManager.isBatchSyncing());
        status.put("shuttingDown", shutdownStateManager.isShuttingDown());
        status.put("activeTasks", shutdownStateManager.getActiveTaskCount());
        status.put("currentOrderId", shutdownStateManager.getCurrentProcessingOrderId());
        status.put("currentSyncingStoreCount", shutdownStateManager.getCurrentSyncingStoreCount());
        status.put("currentSyncingStores", shutdownStateManager.getCurrentSyncingStores());
        status.put("batchSyncStartTime", shutdownStateManager.getBatchSyncStartTime());
        status.put("statusInfo", shutdownStateManager.getStatusInfo());
        return CommonResult.success(status);
    }

    @GetMapping("/sync/batch-progress")
    @Operation(summary = "查询门店同步进度（批量同步）")
    @PermitAll
    public CommonResult<java.util.Map<String, Object>> getBatchSyncProgress() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("isSyncing", shutdownStateManager.isBatchSyncing());
        result.put("syncStatus", shutdownStateManager.getSyncStatus());
        result.put("totalStores", shutdownStateManager.getTotalStoreCount());
        result.put("completedStores", shutdownStateManager.getCompletedStoreCount());
        result.put("successStores", shutdownStateManager.getSuccessStoreCount());
        result.put("failedStores", shutdownStateManager.getFailedStoreCount());
        result.put("currentSyncingCount", shutdownStateManager.getCurrentSyncingStoreCount());
        result.put("currentSyncingStores", shutdownStateManager.getCurrentSyncingStores());
        result.put("startTime", shutdownStateManager.getBatchSyncStartTime());
        result.put("totalOrders", shutdownStateManager.getTotalOrderCount());
        result.put("successOrders", shutdownStateManager.getSuccessOrderCount());
        result.put("failOrders", shutdownStateManager.getFailOrderCount());
        return CommonResult.success(result);
    }

    @PostMapping("/sync/reset-status")
    @Operation(summary = "重置同步状态（用于状态卡死时手动恢复）")
    @PermitAll
    public CommonResult<Boolean> resetSyncStatus() {
        shutdownStateManager.finishBatchSync();
        return CommonResult.success(true);
    }

    @GetMapping("/sync/progress")
    @Operation(summary = "查询同步/补偿进度")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleCompensateProgressDTO> getSyncProgress(
            @Parameter(description = "任务ID", required = true) @RequestParam String taskId) {
        return CommonResult.success(eleOrderService.getSyncProgress(taskId));
    }

    @GetMapping("/sync/redis-progress")
    @Operation(summary = "查询Redis缓存同步进度")
    @PermitAll
    public CommonResult<java.util.Map<String, Object>> getRedisSyncProgress(
            @Parameter(description = "批次ID") @RequestParam String batchId,
            @Parameter(description = "门店ID") @RequestParam(required = false) String platformStoreId) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (platformStoreId != null) {
            StoreSyncProgress progress = orderSyncProgressCache.getStoreProgress(batchId, platformStoreId);
            result.put("storeProgress", progress);
        } else {
            java.util.List<StoreSyncProgress> progressList = orderSyncProgressCache.getBatchProgress(batchId);
            result.put("storeProgressList", progressList);
        }
        return CommonResult.success(result);
    }

    @GetMapping("/fail-record/list")
    @Operation(summary = "查询失败记录")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<List<EleOrderFailRecordRespVO>> getFailRecords() {
        return CommonResult.success(BeanUtils.toBean(eleOrderService.getFailRecords(), EleOrderFailRecordRespVO.class));
    }

    @PostMapping("/fail-record/retry")
    @Operation(summary = "重试失败记录")
    @PreAuthorize("@ss.hasPermission('ele:order:retry')")
    public CommonResult<Boolean> retryFailRecord(
            @Parameter(description = "失败记录ID", required = true) @RequestParam Long id) {
        eleOrderService.retryFailRecord(id);
        return CommonResult.success(true);
    }

    @PostMapping("/fail-record/retry-with-overwrite")
    @Operation(summary = "重试失败记录（覆盖已存在订单）")
    @PreAuthorize("@ss.hasPermission('ele:order:retry')")
    public CommonResult<Boolean> retryFailRecordWithOverwrite(
            @Parameter(description = "失败记录ID", required = true) @RequestParam Long id) {
        eleOrderService.retryFailRecord(id, true);
        return CommonResult.success(true);
    }

    @GetMapping("/fail-record/page")
    @Operation(summary = "分页查询失败记录")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleOrderFailRecordRespVO>> getFailRecordPage(
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "内部订单号") @RequestParam(required = false) String orderId,
            @Parameter(description = "平台订单号") @RequestParam(required = false) String channelOrderId,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "失败阶段") @RequestParam(required = false) String failStage,
            @Parameter(description = "处理状态") @RequestParam(required = false) String processStatus,
            @Parameter(description = "起始时间(秒级时间戳)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间(秒级时间戳)") @RequestParam(required = false) Long endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {

        PageResult<EleOrderFailRecordRespVO> result = eleOrderService.getFailRecordPage(
                storeId, orderId, channelOrderId, bizType, failStage, processStatus, startTime, endTime, pageNo,
                pageSize);
        return CommonResult.success(result);
    }

    @PostMapping("/fail-record/batch-retry")
    @Operation(summary = "批量重试失败记录")
    @PreAuthorize("@ss.hasPermission('ele:order:retry')")
    public CommonResult<Boolean> batchRetryFailRecord(
            @Parameter(description = "失败记录ID列表") @RequestParam List<Long> ids) {
        eleOrderService.batchRetryFailRecord(ids);
        return CommonResult.success(true);
    }

    @GetMapping("/fail-record/unhandled-count")
    @Operation(summary = "获取未处理失败记录统计")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<Map<String, Integer>> getUnhandledFailCount() {
        return CommonResult.success(eleOrderService.getUnhandledFailCount());
    }

    @GetMapping("/fail-record/all-failed-ids")
    @Operation(summary = "获取所有FAILED状态的失败记录ID")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<List<Long>> getAllFailedIds() {
        return CommonResult.success(eleOrderService.getAllFailedIds());
    }

    @PostMapping("/fail-record/retry-by-specified-time")
    @Operation(summary = "按指定时间点批量重试失败记录")
    @PreAuthorize("@ss.hasPermission('ele:order:retry')")
    public CommonResult<Integer> retryFailRecordsBySpecifiedTime(
            @Parameter(description = "指定时间点(毫秒级时间戳)", required = true) @RequestParam Long specifiedTime,
            @Parameter(description = "是否覆盖已存在订单") @RequestParam(defaultValue = "false") Boolean overwrite) {
        int count = eleOrderService.retryAllFailedRecordsBySpecifiedTime(specifiedTime, overwrite);
        return CommonResult.success(count);
    }

    @GetMapping("/status-log/list")
    @Operation(summary = "查询订单状态日志")
    @PermitAll
    public CommonResult<List<EleOrderStatusLogRespVO>> getStatusLogs(
            @Parameter(description = "订单ID", required = true) @RequestParam String orderId) {
        return CommonResult
                .success(BeanUtils.toBean(eleOrderService.getStatusLogs(orderId), EleOrderStatusLogRespVO.class));
    }

    @GetMapping("/sync/config")
    @Operation(summary = "获取订单同步配置信息")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<Map<String, Object>> getSyncConfig() {
        return CommonResult.success(eleOrderService.getSyncConfig());
    }

    @GetMapping("/sync/schedule-config")
    @Operation(summary = "查询定时同步配置")
    @PermitAll
    public CommonResult<Map<String, Object>> getSyncScheduleConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        long intervalSeconds = eleOrderSchedulerProperties.getIntervalSeconds();
        long intervalHours = Math.max(1, intervalSeconds / 3600);

        result.put("exists", true);
        result.put("enabled", eleOrderSchedulerProperties.isEnabled());
        result.put("scheduleType", "interval");
        result.put("intervalSeconds", intervalSeconds);
        result.put("intervalHours", intervalHours);
        result.put("initialDelaySeconds", eleOrderSchedulerProperties.getInitialDelaySeconds());
        result.put("cronExpression", null);
        result.put("source", "application-config");
        return CommonResult.success(result);
    }

    @PutMapping("/sync/schedule-config")
    @Operation(summary = "更新定时同步配置")
    @PermitAll
    public CommonResult<Boolean> updateSyncScheduleConfig(
            @RequestBody EleOrderScheduleConfigReqVO reqVO) {
        return CommonResult.error(400, "定时同步间隔已改为后端配置文件控制，请修改 ele.order.scheduler 配置后重启服务");
    }

    @GetMapping("/list/all")
    @Operation(summary = "查询所有门店的订单列表及详情")
    @PermitAll
    public CommonResult<List<OrderListRespDTO.OrderDetail>> getAllStoreOrdersWithDetails() {
        return CommonResult.success(eleOrderService.getAllStoreOrdersWithDetails());
    }

    @ExceptionHandler(EleOrderSyncException.class)
    public CommonResult<Boolean> handleEleOrderSyncException(EleOrderSyncException e) {
        return CommonResult.error(EleOrderSyncException.ERROR_CODE, e.getMessage());
    }

    @GetMapping("/push-setting")
    @Operation(summary = "获取当前用户订单推送设置")
    @PermitAll
    public CommonResult<OrderPushSettingVO> getOrderPushSetting() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return CommonResult.error(401, "未登录");
        }

        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }

        OrderPushSettingVO vo = new OrderPushSettingVO();
        vo.setOrderPushEnabled(getOrderPushEnabled(user));
        vo.setOrderPushTypes(getOrderPushTypes(user));
        vo.setOrderPushSound(getOrderPushSound(user));
        vo.setOrderPushDesktop(getOrderPushDesktop(user));

        return CommonResult.success(vo);
    }

    @PostMapping("/update-push-setting")
    @Operation(summary = "更新当前用户订单推送设置")
    @PermitAll
    public CommonResult<Boolean> updateOrderPushSetting(@RequestBody OrderPushSettingVO vo) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return CommonResult.error(401, "未登录");
        }

        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }

        Boolean pushEnabled = vo.getOrderPushEnabled() != null ? vo.getOrderPushEnabled() : true;
        String pushTypes = vo.getOrderPushTypes();
        Boolean pushSound = vo.getOrderPushSound() != null ? vo.getOrderPushSound() : true;
        Boolean pushDesktop = vo.getOrderPushDesktop() != null ? vo.getOrderPushDesktop() : false;

        String sql = "UPDATE system_users SET order_push_enabled = ?, order_push_types = ?, " +
                "order_push_sound = ?, order_push_desktop = ?, updater = ?, update_time = NOW() " +
                "WHERE id = ?";
        jdbcTemplate.update(sql, pushEnabled ? 1 : 0, pushTypes, pushSound ? 1 : 0,
                pushDesktop ? 1 : 0, userId, userId);

        return CommonResult.success(true);
    }

    private Boolean getOrderPushEnabled(AdminUserRespDTO user) {
        try {
            Object enabled = user.getClass().getMethod("getOrderPushEnabled").invoke(user);
            return enabled != null ? (Boolean) enabled : true;
        } catch (Exception e) {
            return true;
        }
    }

    private String getOrderPushTypes(AdminUserRespDTO user) {
        try {
            Object types = user.getClass().getMethod("getOrderPushTypes").invoke(user);
            return types != null ? (String) types : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getOrderPushSound(AdminUserRespDTO user) {
        try {
            Object sound = user.getClass().getMethod("getOrderPushSound").invoke(user);
            return sound != null ? (Boolean) sound : true;
        } catch (Exception e) {
            return true;
        }
    }

    private Boolean getOrderPushDesktop(AdminUserRespDTO user) {
        try {
            Object desktop = user.getClass().getMethod("getOrderPushDesktop").invoke(user);
            return desktop != null ? (Boolean) desktop : false;
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/bill-info")
    @Operation(summary = "查询订单账单信息")
    @Parameter(name = "orderId", description = "订单号", required = true, example = "123456")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<OrderBillVO> getOrderBillInfo(@RequestParam("orderId") String orderId) {
        OrderBillVO billInfo = eleBillSyncService.getOrderBillInfo(orderId);
        return CommonResult.success(billInfo);
    }

}
