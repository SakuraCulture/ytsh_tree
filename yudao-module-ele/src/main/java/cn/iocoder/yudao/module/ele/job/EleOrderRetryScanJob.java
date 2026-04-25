package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 翱象订单失败重试扫描定时任务
 *
 * 定时扫描失败订单记录，自动重新提交处理。
 *
 * @author 优团科技数字化团队
 */
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
