package cn.iocoder.yudao.module.system.job.logger;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.infra.framework.log.config.LogCleanProperties;
import cn.iocoder.yudao.module.system.service.logger.OperateLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 物理删除 N 天前的操作日志的 Job
 *
 * @author 优团科技数字化团队
 */
@Slf4j
@Component
public class OperateLogCleanJob implements JobHandler {

    @Resource
    private OperateLogService operateLogService;

    @Resource
    private LogCleanProperties logCleanProperties;

    @Override
    @TenantIgnore
    public String execute(String param) {
        Integer count = operateLogService.cleanOperateLog(logCleanProperties.getOperateLogKeepDays(), logCleanProperties.getDeleteLimit());
        log.info("[execute][定时执行清理操作日志数量 ({}) 个]", count);
        return String.format("定时执行清理操作日志数量 %s 个", count);
    }

}
