package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.ThreadPoolAlarmThresholdReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.ThreadPoolStatusRespVO;
import cn.iocoder.yudao.module.ele.service.threadpool.AdaptivePoolManager;
import cn.iocoder.yudao.module.ele.service.threadpool.TaskRateLimiter;
import cn.iocoder.yudao.module.ele.service.threadpool.ThreadPoolAlarmConfigService;
import cn.iocoder.yudao.module.ele.service.threadpool.ThreadPoolAlarmConfigService.PoolAlarmConfig;
import cn.iocoder.yudao.module.ele.service.threadpool.ThreadPoolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Tag(name = "管理后台 - 线程池监控")
@RestController
@RequestMapping("/ele/thread-pool")
@Validated
@TenantIgnore
public class ThreadPoolController {

    @Resource
    private ThreadPoolRegistry threadPoolRegistry;

    @Resource
    private ThreadPoolAlarmConfigService alarmConfigService;

    @Resource
    private TaskRateLimiter taskRateLimiter;

    @Resource
    private AdaptivePoolManager adaptivePoolManager;

    @GetMapping("/status")
    @Operation(summary = "获取所有线程池状态")
    public CommonResult<List<ThreadPoolStatusRespVO>> getAllPoolStatus() {
        Map<String, ThreadPoolTaskExecutor> pools = threadPoolRegistry.getAllPools();
        List<ThreadPoolStatusRespVO> result = new ArrayList<>();
        
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : pools.entrySet()) {
            result.add(buildStatusVO(entry.getKey(), entry.getValue()));
        }
        
        return CommonResult.success(result);
    }

    @GetMapping("/status/{poolName}")
    @Operation(summary = "获取单个线程池状态")
    public CommonResult<ThreadPoolStatusRespVO> getPoolStatus(
            @Parameter(description = "线程池Bean名称", required = true) @PathVariable String poolName) {
        ThreadPoolTaskExecutor executor = threadPoolRegistry.getPool(poolName);
        if (executor == null) {
            return CommonResult.error(404, "线程池不存在: " + poolName);
        }
        return CommonResult.success(buildStatusVO(poolName, executor));
    }

    @GetMapping("/alarm-threshold")
    @Operation(summary = "获取所有线程池报警阈值配置")
    public CommonResult<Map<String, PoolAlarmConfig>> getAllAlarmThresholds() {
        return CommonResult.success(alarmConfigService.getAllConfigs());
    }

    @PutMapping("/alarm-threshold")
    @Operation(summary = "设置线程池报警阈值（前端可调用）")
    public CommonResult<Boolean> setAlarmThreshold(@Valid @RequestBody ThreadPoolAlarmThresholdReqVO reqVO) {
        alarmConfigService.updateAlarmConfig(
                reqVO.getPoolName(),
                reqVO.getQueueThresholdPercent(),
                reqVO.getActiveThresholdPercent(),
                reqVO.getEnabled()
        );
        return CommonResult.success(true);
    }

    @GetMapping("/health")
    @Operation(summary = "线程池健康检查")
    public CommonResult<Map<String, Object>> healthCheck() {
        Map<String, ThreadPoolTaskExecutor> pools = threadPoolRegistry.getAllPools();
        Map<String, Object> healthResult = new LinkedHashMap<>();
        
        int healthyCount = 0;
        int warningCount = 0;
        int criticalCount = 0;
        
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : pools.entrySet()) {
            String status = checkHealth(entry.getKey(), entry.getValue());
            healthResult.put(entry.getKey(), status);
            
            if (status.contains("HEALTHY")) {
                healthyCount++;
            } else if (status.contains("CRITICAL")) {
                criticalCount++;
            } else {
                warningCount++;
            }
        }
        
        healthResult.put("_summary", Map.of(
                "total", pools.size(),
                "healthy", healthyCount,
                "warning", warningCount,
                "critical", criticalCount,
                "overall", criticalCount > 0 ? "CRITICAL" : warningCount > 0 ? "WARNING" : "HEALTHY"
        ));
        
        return CommonResult.success(healthResult);
    }

    private ThreadPoolStatusRespVO buildStatusVO(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor inner = executor.getThreadPoolExecutor();
        ThreadPoolStatusRespVO vo = new ThreadPoolStatusRespVO();
        
        vo.setPoolName(poolName);
        vo.setCorePoolSize(executor.getCorePoolSize());
        vo.setMaxPoolSize(executor.getMaxPoolSize());
        vo.setPoolSize(inner.getPoolSize());
        vo.setActiveCount(inner.getActiveCount());
        vo.setQueueSize(inner.getQueue().size());
        vo.setQueueCapacity(executor.getQueueCapacity());
        vo.setCompletedTaskCount(inner.getCompletedTaskCount());
        vo.setTaskCount(inner.getTaskCount());
        vo.setRejectedPolicy(inner.getRejectedExecutionHandler().getClass().getSimpleName());
        
        int queueCapacity = executor.getQueueCapacity();
        double queueUsage = queueCapacity > 0 ? (inner.getQueue().size() * 100.0 / queueCapacity) : 0;
        vo.setQueueUsagePercent(Math.round(queueUsage * 10.0) / 10.0);
        
        int poolSize = inner.getPoolSize();
        double activePercent = poolSize > 0 ? (inner.getActiveCount() * 100.0 / poolSize) : 0;
        vo.setActivePercent(Math.round(activePercent * 10.0) / 10.0);
        
        PoolAlarmConfig config = alarmConfigService.getAlarmConfig(poolName);
        String[] health = evaluateHealth(queueUsage, activePercent, config);
        vo.setHealthStatus(health[0]);
        vo.setHealthMessage(health[1]);
        
        return vo;
    }

    private String[] evaluateHealth(double queueUsage, double activePercent, PoolAlarmConfig config) {
        if (!config.isEnabled()) {
            return new String[]{"HEALTHY", "报警已禁用"};
        }
        
        // 严重告警：队列积压且线程满载，说明处理能力不足
        boolean criticalByBoth = queueUsage >= 80 && activePercent >= 90;
        boolean criticalByQueue = queueUsage >= 95;
        
        if (queueUsage >= config.getQueueThresholdPercent() || activePercent >= config.getActiveThresholdPercent()) {
            if (criticalByBoth || criticalByQueue) {
                return new String[]{"CRITICAL", 
                    String.format("队列使用%.0f%%(阈值%d%%), 活跃率%.0f%%(阈值%d%%)",
                        queueUsage, config.getQueueThresholdPercent(),
                        activePercent, config.getActiveThresholdPercent())};
            }
            return new String[]{"WARNING", 
                String.format("队列使用%.0f%%(阈值%d%%), 活跃率%.0f%%(阈值%d%%)",
                    queueUsage, config.getQueueThresholdPercent(),
                    activePercent, config.getActiveThresholdPercent())};
        }
        
        return new String[]{"HEALTHY", 
            String.format("队列使用%.0f%%, 活跃率%.0f%%", queueUsage, activePercent)};
    }

    private String checkHealth(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor inner = executor.getThreadPoolExecutor();
        int queueCapacity = executor.getQueueCapacity();
        double queueUsage = queueCapacity > 0 ? (inner.getQueue().size() * 100.0 / queueCapacity) : 0;
        double activePercent = inner.getPoolSize() > 0 ? (inner.getActiveCount() * 100.0 / inner.getPoolSize()) : 0;
        
        PoolAlarmConfig config = alarmConfigService.getAlarmConfig(poolName);
        String[] health = evaluateHealth(queueUsage, activePercent, config);
        return health[0] + ": " + health[1];
    }

    @GetMapping("/rate-limiter/status")
    @Operation(summary = "获取限流器状态")
    public CommonResult<Map<String, Object>> getRateLimiterStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentConcurrentTasks", taskRateLimiter.getCurrentConcurrentTasks());
        status.put("maxConcurrentTasks", taskRateLimiter.getMaxConcurrentTasks());
        status.put("utilizationPercent", Math.round(taskRateLimiter.getCurrentConcurrentTasks() * 100.0 / taskRateLimiter.getMaxConcurrentTasks() * 10.0) / 10.0);
        return CommonResult.success(status);
    }

    @PostMapping("/rate-limiter/set-max")
    @Operation(summary = "动态调整限流器最大并发数")
    public CommonResult<Boolean> setRateLimiterMax(@RequestParam int maxConcurrent) {
        if (maxConcurrent < 1 || maxConcurrent > 200) {
            return CommonResult.error(400, "最大并发数必须在1-200之间");
        }
        taskRateLimiter.setMaxConcurrent(maxConcurrent);
        return CommonResult.success(true);
    }

    @PostMapping("/pool/scale-up")
    @Operation(summary = "手动扩容线程池")
    public CommonResult<Boolean> scaleUpPool(@RequestParam int targetCoreSize) {
        if (targetCoreSize < 1 || targetCoreSize > 50) {
            return CommonResult.error(400, "核心线程数必须在1-50之间");
        }
        adaptivePoolManager.scaleUp(targetCoreSize);
        return CommonResult.success(true);
    }

    @PostMapping("/pool/scale-down")
    @Operation(summary = "手动缩容线程池")
    public CommonResult<Boolean> scaleDownPool(@RequestParam int targetCoreSize) {
        if (targetCoreSize < 1 || targetCoreSize > 50) {
            return CommonResult.error(400, "核心线程数必须在1-50之间");
        }
        adaptivePoolManager.scaleDown(targetCoreSize);
        return CommonResult.success(true);
    }
}
