package cn.iocoder.yudao.module.ele.job;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.config.EleStoreGoodsFullSyncJobProperties;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreGoodsFullSyncExecutor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class EleStoreGoodsFullSyncJob {

    private static final Long ELE_PLATFORM_ID = 1L;

    @Resource
    private EleStoreGoodsFullSyncJobProperties properties;
    @Resource
    private StoreService storeService;
    @Resource
    private EleStoreGoodsFullSyncExecutor fullSyncExecutor;
    @Resource
    private ApplicationContext applicationContext;

    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduleTask;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("[商品全量同步定时任务] 已禁用");
            return;
        }

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ele-goods-full-sync-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(120);
        scheduler.initialize();
        this.taskScheduler = scheduler;

        scheduleTask = taskScheduler.schedule(this::run, new CronTrigger(properties.getCron()));
        log.info("[商品全量同步定时任务] 已注册, cron={}, ckSyncEnabled={}", properties.getCron(), properties.isCkSyncEnabled());
    }

    @PreDestroy
    public void destroy() {
        if (scheduleTask != null) {
            scheduleTask.cancel(false);
        }
        if (taskScheduler != null) {
            taskScheduler.destroy();
        }
    }

    private void run() {
        long totalStart = System.currentTimeMillis();
        log.info("==========================================");
        log.info("[商品全量同步定时任务] ========== 开始执行 ==========");
        log.info("==========================================");

        try {
            List<StorePlatformRespVO> stores = storeService.getAllPlatformStores(ELE_PLATFORM_ID);
            if (CollUtil.isEmpty(stores)) {
                log.warn("[商品全量同步定时任务] 没有可同步的饿了么门店");
                return;
            }
            log.info("[商品全量同步定时任务] 查询到 {} 个门店, 耗时={}ms", stores.size(), System.currentTimeMillis() - totalStart);

            log.info("[商品全量同步定时任务] ========== 开始拉取商品 ==========");
            fullSyncExecutor.executeDirectly(stores, false);
            long fetchDuration = System.currentTimeMillis() - totalStart;
            log.info("[商品全量同步定时任务] ========== 商品拉取完成, 耗时={}ms ({}min) ==========",
                    fetchDuration, fetchDuration / 60000);

            if (properties.isCkSyncEnabled()) {
                triggerCkSync(totalStart);
            } else {
                log.info("[商品全量同步定时任务] CK 同步未启用, 跳过");
            }

            long totalDuration = System.currentTimeMillis() - totalStart;
            log.info("==========================================");
            log.info("[商品全量同步定时任务] ========== 全部完成, 总耗时={}ms ({}min) ==========",
                    totalDuration, totalDuration / 60000);
            log.info("==========================================");
        } catch (Exception e) {
            log.error("[商品全量同步定时任务] 执行失败", e);
        }
    }

    private void triggerCkSync(long totalStart) {
        try {
            Object runner = applicationContext.getBean("storeGoodsWideSyncRunner");
            runner.getClass().getMethod("runFullSync").invoke(runner);
            long ckDuration = System.currentTimeMillis() - totalStart;
            log.info("[商品全量同步定时任务] ========== ClickHouse 同步完成, 累计耗时={}ms ==========", ckDuration);
        } catch (Exception e) {
            log.error("[商品全量同步定时任务] ClickHouse 同步失败", e);
        }
    }
}
