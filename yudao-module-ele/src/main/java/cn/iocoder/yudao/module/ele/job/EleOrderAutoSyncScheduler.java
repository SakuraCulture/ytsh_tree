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

/**
 * 饿了么订单自动同步调度器
 *
 * 程序启动后自动执行订单同步，核心逻辑：
 * 1. 启动后延迟指定时间执行首次拉取
 * 2. 首次拉取时各门店从本月1号0点（或上次同步时间）开始拉取
 * 3. 后续按固定间隔周期执行，各门店自动从上次同步结束时间继续拉取
 *
 * @author 优团科技数字化团队
 */
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

        // 启动后延迟执行首次同步（传入null，让增量逻辑自动从上次同步时间继续）
        scheduler.schedule(this::executeSync, initialDelaySeconds, TimeUnit.SECONDS);

        // 启动周期性定时任务
        scheduler.scheduleAtFixedRate(
                this::executeSync,
                initialDelaySeconds + intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    /**
     * 执行全部门店订单同步
     * 传入 null 时间参数，让 EleOrderServiceImpl 自动从各门店上次同步时间继续拉取
     */
    private void executeSync() {
        try {
            log.info("【订单定时同步】开始执行全部门店同步");

            List<StorePlatformRespVO> stores = storeService.getAllPlatformStoresByPlatformCode(null);
            if (stores == null || stores.isEmpty()) {
                log.warn("【订单定时同步】无门店数据，跳过同步");
                return;
            }

            log.info("【订单定时同步】共{}家门店需要同步", stores.size());

            // 传入 null, null 触发增量同步逻辑（从各门店上次 sync_time 继续）
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
