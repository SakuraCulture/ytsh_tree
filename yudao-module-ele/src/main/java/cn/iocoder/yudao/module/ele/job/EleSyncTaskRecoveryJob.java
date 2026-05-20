package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.service.recovery.SyncTaskRecoveryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class EleSyncTaskRecoveryJob implements JobHandler {

    @Resource
    private SyncTaskRecoveryService syncTaskRecoveryService;

    @Override
    @TenantJob
    public String execute(String param) {
        return syncTaskRecoveryService.recoverTasks();
    }
}
