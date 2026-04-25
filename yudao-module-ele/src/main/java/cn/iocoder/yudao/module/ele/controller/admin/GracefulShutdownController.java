package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.service.ShutdownStateManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 优雅关闭管理接口
 *
 * 提供优雅停止、强制停止、状态查询等接口，
 * 确保应用关闭时不会造成订单数据脏数据。
 *
 * @author 优团科技数字化团队
 */
@Tag(name = "管理后台 - 优雅关闭管理")
@RestController
@RequestMapping("/admin-api/ele/shutdown")
@Slf4j
@TenantIgnore
public class GracefulShutdownController {

    @Resource
    private ShutdownStateManager shutdownStateManager;

    @Resource(name = "eleOrderSyncExecutor")
    private ThreadPoolTaskExecutor syncExecutor;

    @Resource(name = "eleOrderCompensateExecutor")
    private ThreadPoolTaskExecutor compensateExecutor;

    @Resource(name = "eleOrderRetryExecutor")
    private ThreadPoolTaskExecutor retryExecutor;

    @Value("${ele.order.shutdown.graceful-timeout-seconds:60}")
    private int gracefulTimeoutSeconds;

    @Value("${ele.order.shutdown.force-shutdown-after-seconds:90}")
    private int forceShutdownAfterSeconds;

    @Value("${ele.order.shutdown.enabled:true}")
    private boolean shutdownEnabled;

    /**
     * 优雅停止接口
     *
     * 触发优雅停止流程：
     * 1. 设置停机标志，拒绝新任务
     * 2. 等待当前批次完成
     * 3. 关闭线程池
     * 4. 返回状态
     */
    @PostMapping("/graceful")
    @Operation(summary = "优雅停止")
    public CommonResult<String> gracefulShutdown() {
        if (!shutdownEnabled) {
            return CommonResult.error(400, "优雅关闭功能未启用");
        }

        if (shutdownStateManager.isShuttingDown()) {
            return CommonResult.success("优雅停止已在进行中，请勿重复触发");
        }

        log.info("【优雅关闭】触发优雅停止请求");

        new Thread(() -> {
            try {
                executeGracefulShutdown();
            } catch (Exception e) {
                log.error("【优雅关闭】执行异常", e);
            }
        }, "graceful-shutdown-thread").start();

        return CommonResult.success("优雅停止已触发，正在等待任务完成，预计需要 " + gracefulTimeoutSeconds + " 秒");
    }

    /**
     * 强制停止接口
     *
     * 立即关闭所有线程池，不等待任务完成。
     * 仅在紧急场景使用，可能导致部分订单数据不完整。
     */
    @PostMapping("/force")
    @Operation(summary = "强制停止")
    public CommonResult<String> forceShutdown() {
        if (!shutdownEnabled) {
            return CommonResult.error(400, "优雅关闭功能未启用");
        }

        log.warn("【优雅关闭】触发强制停止请求，立即关闭应用");

        shutdownStateManager.triggerShutdown();

        syncExecutor.getThreadPoolExecutor().shutdownNow();
        compensateExecutor.getThreadPoolExecutor().shutdownNow();
        retryExecutor.getThreadPoolExecutor().shutdownNow();

        return CommonResult.success("强制停止已触发，应用将立即关闭");
    }

    /**
     * 查询停机状态接口
     */
    @GetMapping("/status")
    @Operation(summary = "查询停机状态")
    public CommonResult<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("shuttingDown", shutdownStateManager.isShuttingDown());
        status.put("activeTasks", shutdownStateManager.getActiveTaskCount());
        status.put("currentProcessingOrderId", shutdownStateManager.getCurrentProcessingOrderId());
        status.put("syncExecutorActive", !syncExecutor.getThreadPoolExecutor().isShutdown());
        status.put("compensateExecutorActive", !compensateExecutor.getThreadPoolExecutor().isShutdown());
        status.put("retryExecutorActive", !retryExecutor.getThreadPoolExecutor().isShutdown());
        status.put("shutdownEnabled", shutdownEnabled);
        status.put("detailedStatus", shutdownStateManager.getStatusInfo());

        return CommonResult.success(status);
    }

    /**
     * 执行优雅停止流程
     */
    private void executeGracefulShutdown() {
        long startTime = System.currentTimeMillis();
        long timeoutMs = gracefulTimeoutSeconds * 1000L;

        log.info("【优雅关闭】开始执行优雅停止流程");

        try {
            // 1. 设置停机标志，拒绝新任务
            shutdownStateManager.triggerShutdown();
            log.info("【优雅关闭】已设置停机标志，拒绝新任务");

            // 2. 等待当前批次完成（最多等待 30 秒）
            log.info("【优雅关闭】等待当前批次任务完成...");
            boolean batchCompleted = shutdownStateManager.waitForTasks(30000);

            if (!batchCompleted) {
                log.warn("【优雅关闭】批次任务等待超时，准备强制关闭线程池");
            }

            // 3. 关闭同步线程池
            log.info("【优雅关闭】开始关闭同步线程池");
            syncExecutor.getThreadPoolExecutor().shutdown();
            boolean syncClosed = syncExecutor.getThreadPoolExecutor().awaitTermination(20, TimeUnit.SECONDS);
            if (!syncClosed) {
                log.warn("【优雅关闭】同步线程池关闭超时，执行强制关闭");
                syncExecutor.getThreadPoolExecutor().shutdownNow();
            }

            // 4. 关闭补偿线程池
            log.info("【优雅关闭】开始关闭补偿线程池");
            compensateExecutor.getThreadPoolExecutor().shutdown();
            boolean compensateClosed = compensateExecutor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS);
            if (!compensateClosed) {
                log.warn("【优雅关闭】补偿线程池关闭超时，执行强制关闭");
                compensateExecutor.getThreadPoolExecutor().shutdownNow();
            }

            // 5. 关闭重试线程池
            log.info("【优雅关闭】开始关闭重试线程池");
            retryExecutor.getThreadPoolExecutor().shutdown();
            boolean retryClosed = retryExecutor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS);
            if (!retryClosed) {
                log.warn("【优雅关闭】重试线程池关闭超时，执行强制关闭");
                retryExecutor.getThreadPoolExecutor().shutdownNow();
            }

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            log.info("【优雅关闭】优雅停止流程完成，耗时{}秒", elapsed);

        } catch (Exception e) {
            log.error("【优雅关闭】执行优雅停止异常", e);
        }
    }
}
