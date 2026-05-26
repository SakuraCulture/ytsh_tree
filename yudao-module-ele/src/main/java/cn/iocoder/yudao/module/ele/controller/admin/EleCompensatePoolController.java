package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;


@Tag(name = "管理后台 - 饿了么补偿任务线程池")
@RestController
@RequestMapping("/ele/compensate/pool")
@TenantIgnore
public class EleCompensatePoolController {

    @Resource
    @Qualifier("eleOrderCompensateExecutor")
    private ThreadPoolTaskExecutor compensateExecutor;

    @GetMapping("/stats")
    @Operation(summary = "获取补偿任务线程池状态")
    public CommonResult<Map<String, Object>> getPoolStats() {
        ThreadPoolExecutor executor = compensateExecutor.getThreadPoolExecutor();

        Map<String, Object> stats = new HashMap<>();
        stats.put("corePoolSize", compensateExecutor.getCorePoolSize());
        stats.put("maxPoolSize", compensateExecutor.getMaxPoolSize());
        stats.put("poolSize", executor.getPoolSize());
        stats.put("activeCount", executor.getActiveCount());
        stats.put("queueSize", executor.getQueue().size());
        stats.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
        stats.put("completedTaskCount", executor.getCompletedTaskCount());
        stats.put("taskCount", executor.getTaskCount());

        return CommonResult.success(stats);
    }

    @PostMapping("/resize")
    @Operation(summary = "动态调整补偿任务线程池大小")
    public CommonResult<Boolean> resizePool(
            @RequestParam(required = false) Integer coreSize,
            @RequestParam(required = false) Integer maxSize) {

        if (coreSize != null) {
            compensateExecutor.setCorePoolSize(coreSize);
        }
        if (maxSize != null) {
            compensateExecutor.setMaxPoolSize(maxSize);
        }
        compensateExecutor.afterPropertiesSet();

        return CommonResult.success(true);
    }

}
