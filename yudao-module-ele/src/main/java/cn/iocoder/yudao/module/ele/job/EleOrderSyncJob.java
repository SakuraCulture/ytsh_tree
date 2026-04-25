package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 翱象订单同步定时任务
 *
 * 通过定时任务定期从翱象平台拉取订单数据到本地。
 *
 * @author 优团科技数字化团队
 */
@Component
public class EleOrderSyncJob implements JobHandler {

    @Resource
    private EleOrderService eleOrderService;

    @Override
    @TenantJob
    public String execute(String param) {
        eleOrderService.syncAllStores();
        return "订单同步完成";
    }

}
