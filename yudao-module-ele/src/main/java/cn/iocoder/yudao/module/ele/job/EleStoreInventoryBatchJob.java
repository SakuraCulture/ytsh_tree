package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.service.EleStoreInventoryBatchService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class EleStoreInventoryBatchJob implements JobHandler {

    @Resource
    private EleStoreInventoryBatchService inventoryBatchService;

    @Override
    @TenantJob
    public String execute(String param) {
        Long taskId = inventoryBatchService.createScheduledAllOpenStoresBatchTask();
        return "库存批量任务已提交, taskId=" + taskId;
    }
}
