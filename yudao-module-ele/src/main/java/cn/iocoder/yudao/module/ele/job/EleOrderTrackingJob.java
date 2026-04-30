package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.module.ele.service.EleOrderTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 订单跟踪定时任务
 * 定时扫描未完结订单，对超时订单进行告警
 */
@Slf4j
@Component
public class EleOrderTrackingJob {

    @Resource
    private EleOrderTrackingService eleOrderTrackingService;

    /**
     * 每10分钟扫描一次超时订单
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void scanTimeoutOrders() {
        log.info("【定时任务】开始执行订单跟踪扫描任务...");
        try {
            eleOrderTrackingService.scanTimeoutOrders();
        } catch (Exception e) {
            log.error("【定时任务】订单跟踪扫描任务执行失败，error={}", e.getMessage(), e);
        }
        log.info("【定时任务】订单跟踪扫描任务执行完成");
    }
}
