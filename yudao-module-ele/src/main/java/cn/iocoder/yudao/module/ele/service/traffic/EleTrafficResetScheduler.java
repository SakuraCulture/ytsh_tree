package cn.iocoder.yudao.module.ele.service.traffic;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EleTrafficResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(EleTrafficResetScheduler.class);

    @Resource
    private EleTrafficMetricsCollector metricsCollector;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyStats() {
        log.info("[流量监控] 每日零点定时重置开始");
        metricsCollector.resetForNewDay();
        log.info("[流量监控] 每日零点定时重置完成");
    }
}
