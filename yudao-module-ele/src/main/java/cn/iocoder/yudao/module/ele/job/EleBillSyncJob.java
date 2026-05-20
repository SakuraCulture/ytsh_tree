package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.config.EleBillSchedulerProperties;
import cn.iocoder.yudao.module.ele.service.EleBillSyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class EleBillSyncJob implements JobHandler {

    @Resource
    private EleBillSyncService eleBillSyncService;
    @Resource
    private EleBillSchedulerProperties billSchedulerProperties;

    @Override
    @TenantJob
    public String execute(String param) {
        String billDate = LocalDate.now().minusDays(billSchedulerProperties.getOffsetDays()).toString();
        log.info("【账单同步定时任务】开始同步账单: {}", billDate);
        try {
            eleBillSyncService.syncAllBillsByDate(billDate);
            return "账单同步完成";
        } catch (Exception e) {
            log.error("【账单同步定时任务】同步失败: {}", e.getMessage(), e);
            return "账单同步失败: " + e.getMessage();
        }
    }
}
