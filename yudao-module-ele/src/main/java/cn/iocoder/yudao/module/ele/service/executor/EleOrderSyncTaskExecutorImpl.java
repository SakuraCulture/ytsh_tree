package cn.iocoder.yudao.module.ele.service.executor;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.ele.service.EleApiRateLimiter;
import cn.iocoder.yudao.module.ele.service.ShutdownStateManager;
import cn.iocoder.yudao.module.ele.service.threadpool.TaskRateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Slf4j
@Service
public class EleOrderSyncTaskExecutorImpl implements EleOrderSyncTaskExecutor {

    private final ThreadPoolTaskExecutor executor;

    private final ShutdownStateManager shutdownStateManager;

    @Resource
    private TaskRateLimiter rateLimiter;

    @Resource
    private EleApiRateLimiter eleApiRateLimiter;

    
    @Value("${ele.order.sync.batch-size:20}")
    private int batchSize;

    @Value("${ele.order.sync.store-concurrent-enabled:false}")
    private boolean storeConcurrentEnabled;

    @Value("${ele.order.sync.store-concurrency:1}")
    private int storeConcurrency;

    public EleOrderSyncTaskExecutorImpl(
            @Qualifier("eleOrderSyncExecutor") ThreadPoolTaskExecutor executor,
            ShutdownStateManager shutdownStateManager) {
        this.executor = executor;
        this.shutdownStateManager = shutdownStateManager;
    }

    @Override
    public SyncResult executeSync(List<StorePlatformRespVO> stores, Long forcedStartTime, Long forcedEndTime) {
        long startTime = System.currentTimeMillis();

        List<StorePlatformRespVO> validStores = stores.stream()
                .filter(s -> StrUtil.isNotBlank(s.getPlatformStoreId()))
                .toList();

        int storeCount = validStores.size();
        if (storeCount == 0) {
            log.info("暂无有效门店需要同步");
            return new SyncResult(0, 0, 0, 0, List.of(), true);
        }

        log.info("开始同步{}家门店订单，门店并发enabled={}，并发数={}，每批{}家...",
                storeCount, storeConcurrentEnabled, Math.max(1, storeConcurrency), batchSize);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> failedStores = new CopyOnWriteArrayList<>();

        Consumer<StorePlatformRespVO> syncAction = store -> {
            String platformStoreId = StrUtil.trim(store.getPlatformStoreId());
            String storeName = StrUtil.trim(store.getPlatformStoreName());
            try {
                shutdownStateManager.registerStoreSyncStarted(platformStoreId, storeName);
                doSyncStore(store, forcedStartTime, forcedEndTime);
                successCount.incrementAndGet();
                shutdownStateManager.registerStoreSyncFinished(platformStoreId, true);
            } catch (Exception e) {
                failCount.incrementAndGet();
                failedStores.add(platformStoreId);
                shutdownStateManager.registerStoreSyncFinished(platformStoreId, false);
                log.error("门店{}同步失败: {}", platformStoreId, e.getMessage(), e);
            }
        };

        if (!storeConcurrentEnabled || storeConcurrency <= 1) {
            executeSerial(validStores, forcedStartTime, forcedEndTime, syncAction, successCount, failCount, failedStores);
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            log.info("门店订单串行同步完成，耗时{}秒，成功{}家，失败{}家",
                    elapsed, successCount.get(), failCount.get());
            return new SyncResult(storeCount, successCount.get(), failCount.get(),
                    elapsed, failedStores, true);
        }

                int concurrentBatchSize = Math.max(1, Math.min(batchSize, storeConcurrency));
        int totalBatches = (storeCount + concurrentBatchSize - 1) / concurrentBatchSize;
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            if (shutdownStateManager.isShuttingDown()) {
                log.warn("【订单同步】应用正在关闭，中断批量同步，剩余{}批未提交", totalBatches - batchIndex);
                break;
            }

            int fromIndex = batchIndex * concurrentBatchSize;
            int toIndex = Math.min(fromIndex + concurrentBatchSize, storeCount);
            List<StorePlatformRespVO> batchStores = validStores.subList(fromIndex, toIndex);

            log.info("提交第{}/{}批任务，门店数: {}", batchIndex + 1, totalBatches, batchStores.size());

            CountDownLatch batchLatch = new CountDownLatch(batchStores.size());
            for (StorePlatformRespVO store : batchStores) {
                if (shutdownStateManager.isShuttingDown()) {
                    log.warn("【订单同步】应用正在关闭，跳过门店同步，platformStoreId={}", store.getPlatformStoreId());
                    batchLatch.countDown();
                    failCount.incrementAndGet();
                    failedStores.add(StrUtil.trim(store.getPlatformStoreId()));
                    continue;
                }

                waitForSubmissionPermit();

                executor.execute(() -> {
                    try {
                        syncAction.accept(store);
                    } finally {
                        batchLatch.countDown();
                        rateLimiter.release();
                    }
                });
            }

                        try {
                long timeoutMinutes = Math.max(5, (batchStores.size() * 30L) / 60);
                boolean completed = batchLatch.await(timeoutMinutes, TimeUnit.MINUTES);
                if (!completed) {
                    long unfinished = batchLatch.getCount();
                    log.warn("第{}批任务超时，{}/{}家门店未完成（超时设置{}分钟）",
                            batchIndex + 1, unfinished, batchStores.size(), timeoutMinutes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("第{}批任务被中断", batchIndex + 1, e);
                break;
            }
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;

        log.info("门店订单同步完成，耗时{}秒，成功{}家，失败{}家",
                elapsed, successCount.get(), failCount.get());

        return new SyncResult(storeCount, successCount.get(), failCount.get(),
                elapsed, failedStores, true);
    }

    
    private StoreSyncDelegate syncDelegate;

    public void setSyncDelegate(StoreSyncDelegate delegate) {
        this.syncDelegate = delegate;
    }

    private void executeSerial(List<StorePlatformRespVO> validStores, Long forcedStartTime, Long forcedEndTime,
            Consumer<StorePlatformRespVO> syncAction, AtomicInteger successCount, AtomicInteger failCount,
            List<String> failedStores) {
        int total = validStores.size();
        for (int i = 0; i < total; i++) {
            StorePlatformRespVO store = validStores.get(i);
            if (shutdownStateManager.isShuttingDown()) {
                log.warn("【门店串行同步】应用正在关闭，跳过剩余{}家门店", total - i);
                break;
            }
            waitForApiBacklogDrainedBeforeSubmit();
            long storeStart = System.currentTimeMillis();
            syncAction.accept(store);
        }
    }

    private void doSyncStore(StorePlatformRespVO store, Long forcedStartTime, Long forcedEndTime) {
        if (syncDelegate == null) {
            throw new IllegalStateException("StoreSyncDelegate 未设置");
        }
        syncDelegate.syncStore(store, forcedStartTime, forcedEndTime);
    }

    private void waitForSubmissionPermit() {
        waitForApiBacklogDrainedBeforeSubmit();

        int waitCount = 0;
        while (!rateLimiter.tryAcquire()) {
            waitCount++;
            if (waitCount > 30) {
                log.error("【限流】等待超过60秒仍未获得提交许可，可能线程池已满载，强制退出同步");
                throw new RuntimeException("线程池满载，任务提交超时");
            }
            if (waitCount % 5 == 0) {
                log.warn("【限流】线程池繁忙(并发{}/{}), 暂停提交, 已等待{}秒",
                        rateLimiter.getCurrentConcurrentTasks(),
                        rateLimiter.getMaxConcurrentTasks(),
                        waitCount * 2);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待限流许可时被中断", e);
            }
        }
    }

    private void waitForApiBacklogDrainedBeforeSubmit() {
        boolean logged = false;
        while (eleApiRateLimiter.hasBacklog()) {
            if (shutdownStateManager.isShuttingDown()) {
                throw new RuntimeException("应用正在关闭，停止等待翱象接口背压恢复");
            }
            if (!logged) {
                log.warn("【翱象背压】门店任务提交暂停，接口请求正在排队，waiting={}",
                        eleApiRateLimiter.getLocalWaitingCount());
                logged = true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待翱象接口排队清空时被中断", e);
            }
        }
        if (logged) {
            log.info("【翱象背压】接口排队清空，恢复门店任务提交");
        }
    }

}
