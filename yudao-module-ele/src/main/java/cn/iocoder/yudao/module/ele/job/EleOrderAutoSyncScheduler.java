package cn.iocoder.yudao.module.ele.job;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.config.EleOrderSchedulerProperties;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor;
import cn.iocoder.yudao.module.ele.service.executor.EleOrderSyncTaskExecutor.SyncResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class EleOrderAutoSyncScheduler implements ApplicationRunner {

    @Resource
    private EleOrderSchedulerProperties schedulerProperties;

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private StoreService storeService;

    @Resource
    private EleOrderSyncTaskExecutor syncTaskExecutor;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ele-order-auto-sync");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void run(ApplicationArguments args) {
        if (!schedulerProperties.isEnabled()) {
            log.info("【订单定时同步】已禁用，跳过启动");
            return;
        }

        long intervalSeconds = schedulerProperties.getIntervalSeconds();
        long initialDelaySeconds = schedulerProperties.getInitialDelaySeconds();

        log.info("【订单定时同步】已启用，间隔={}秒，启动延迟={}秒", intervalSeconds, initialDelaySeconds);

                scheduler.schedule(this::executeSync, initialDelaySeconds, TimeUnit.SECONDS);

                scheduler.scheduleAtFixedRate(
                this::executeSync,
                initialDelaySeconds + intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    
    private void executeSync() {
        try {

            List<StorePlatformRespVO> stores = storeService.getAllPlatformStoresByPlatformCode(null);
            if (stores == null || stores.isEmpty()) {
                log.warn("【订单定时同步】无门店数据，跳过同步");
                return;
            }


                        SyncResult result = syncTaskExecutor.executeSync(stores, null, null);

            log.info("【订单定时同步】完成，门店总数={}，成功={}，失败={}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailCount());

            if (result.getFailCount() > 0) {
                log.warn("【订单定时同步】失败门店: {}", result.getFailedStores());
            }
        } catch (Exception e) {
            log.error("【订单定时同步】执行异常", e);
        }
    }
}
