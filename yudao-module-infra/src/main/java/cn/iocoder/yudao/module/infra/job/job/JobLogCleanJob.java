package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.infra.framework.log.config.LogCleanProperties;
import cn.iocoder.yudao.module.infra.service.job.JobLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 物理删除 N 天前的任务日志的 Job
 *
 * @author j-sentinel
 */
@Slf4j
@Component
public class JobLogCleanJob implements JobHandler {

    @Resource
    private JobLogService jobLogService;

    @Resource
    private LogCleanProperties logCleanProperties;

    @Override
    @TenantIgnore
    public String execute(String param) {
        Integer count = jobLogService.cleanJobLog(logCleanProperties.getJobLogKeepDays(), logCleanProperties.getDeleteLimit());
        log.info("[execute][定时执行清理定时任务日志数量 ({}) 个]", count);
        return String.format("定时执行清理定时任务日志数量 %s 个", count);
    }

}
