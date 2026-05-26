package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


@Component
public class EleOrderRetryScanJob implements JobHandler {

    @Resource
    private EleOrderService eleOrderService;

    @Override
    @TenantJob
    public String execute(String param) {
        eleOrderService.scanPendingRetryRecords();
        return "失败重试扫描完成";
    }

}
