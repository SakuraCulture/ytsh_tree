package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.service.EleOrderRetryQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 饿了么订单线程池状态监控接口
 *
 * 提供同步、补偿、重试三种线程池的状态监控，以及重试队列信息。
 *
 * @author 优团科技数字化团队
 */
@Tag(name = "管理后台 - 饿了么订单线程池监控")
@RestController
@RequestMapping("/ele/order/pool")
@TenantIgnore
public class EleOrderPoolStatusController {

    @Resource
    @Qualifier("eleOrderSyncExecutor")
    private ThreadPoolTaskExecutor syncExecutor;

    @Resource
    @Qualifier("eleOrderCompensateExecutor")
    private ThreadPoolTaskExecutor compensateExecutor;

    @Resource
    @Qualifier("eleOrderRetryExecutor")
    private ThreadPoolTaskExecutor retryExecutor;

    @Resource
    private EleOrderRetryQueueService retryQueueService;

    @GetMapping("/status")
    @Operation(summary = "获取所有线程池状态和重试队列信息")
    public CommonResult<Map<String, Object>> getAllPoolStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("syncPool", buildPoolInfo(syncExecutor));
        result.put("compensatePool", buildPoolInfo(compensateExecutor));
        result.put("retryPool", buildPoolInfo(retryExecutor));
        result.put("retryQueue", buildRetryQueueInfo());
        result.put("health", buildHealthInfo());
        return CommonResult.success(result);
    }

    private Map<String, Object> buildPoolInfo(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();
        int queueSize = threadPool.getQueue().size();
        int queueCapacity = executor.getQueueCapacity();

        Map<String, Object> info = new HashMap<>();
        info.put("corePoolSize", executor.getCorePoolSize());
        info.put("maxPoolSize", executor.getMaxPoolSize());
        info.put("poolSize", threadPool.getPoolSize());
        info.put("activeCount", threadPool.getActiveCount());
        info.put("queueSize", queueSize);
        info.put("queueCapacity", queueCapacity);
        info.put("queueUsagePercent", queueCapacity > 0 ? Math.round((queueSize * 100.0) / queueCapacity) : 0);
        info.put("completedTaskCount", threadPool.getCompletedTaskCount());
        info.put("taskCount", threadPool.getTaskCount());
        return info;
    }

    private Map<String, Object> buildRetryQueueInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalSize", retryQueueService.getQueueSize());
        info.put("isEmpty", retryQueueService.isEmpty());
        info.put("byStore", retryQueueService.getQueueSizeByAllStores());
        return info;
    }

    private Map<String, Object> buildHealthInfo() {
        Map<String, Object> health = new HashMap<>();

        ThreadPoolExecutor retryPool = retryExecutor.getThreadPoolExecutor();
        int retryQueueSize = retryPool.getQueue().size();
        int retryQueueCapacity = retryExecutor.getQueueCapacity();
        boolean retryPoolHealthy = retryQueueSize < retryQueueCapacity - 50;

        health.put("retryPoolHealthy", retryPoolHealthy);
        health.put("retryQueueNearFull", retryQueueSize >= retryQueueCapacity - 50);
        health.put("overallStatus", retryPoolHealthy ? "HEALTHY" : "WARNING");

        return health;
    }
}
