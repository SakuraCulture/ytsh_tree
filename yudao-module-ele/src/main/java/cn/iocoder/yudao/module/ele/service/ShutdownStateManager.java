package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
public class ShutdownStateManager {

    
    private volatile boolean shuttingDown = false;

    
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    
    private volatile String currentProcessingOrderId;

    
    private final AtomicInteger activeBatchSyncCount = new AtomicInteger(0);

    
    private volatile boolean syncingBatch = false;

    
    private final AtomicInteger activeCompensateTasks = new AtomicInteger(0);

    
    private final List<String> syncingStores = new CopyOnWriteArrayList<>();

    
    private final AtomicInteger currentSyncingStoreCount = new AtomicInteger(0);

    
    private volatile long batchSyncStartTime = 0;

    
    private volatile String syncStatus = "IDLE";

    
    private volatile int totalStoreCount = 0;

    
    private final AtomicInteger completedStoreCount = new AtomicInteger(0);

    
    private final AtomicInteger successStoreCount = new AtomicInteger(0);

    
    private final AtomicInteger failedStoreCount = new AtomicInteger(0);

    private final AtomicInteger totalOrderCount = new AtomicInteger(0);
    private final AtomicInteger successOrderCount = new AtomicInteger(0);
    private final AtomicInteger failOrderCount = new AtomicInteger(0);

    
    public void taskStarted(String orderId) {
        activeTasks.incrementAndGet();
        currentProcessingOrderId = orderId;
        log.debug("【优雅关闭】任务开始，orderId={}, 当前活跃任务数={}", orderId, activeTasks.get());
    }

    
    public void taskFinished() {
        activeTasks.decrementAndGet();
        if (activeTasks.get() == 0) {
            currentProcessingOrderId = null;
        }
        log.debug("【优雅关闭】任务结束，当前活跃任务数={}", activeTasks.get());
    }

    
    public boolean canAcceptNewTask() {
        return !shuttingDown;
    }

    
    public boolean canAcceptNewBatch() {
        return !shuttingDown && activeBatchSyncCount.get() == 0;
    }

    
    public synchronized boolean startBatchSync(int totalStores) {
        if (shuttingDown) {
            log.warn("【优雅关闭】应用正在关闭，拒绝新的批量同步任务");
            return false;
        }
        if (syncingBatch) {
            log.warn("【批次控制】已有批次在执行，拒绝新的批量同步任务");
            return false;
        }
        syncingBatch = true;
        syncStatus = "RUNNING";
        batchSyncStartTime = System.currentTimeMillis();
        totalStoreCount = totalStores;
        completedStoreCount.set(0);
        successStoreCount.set(0);
        failedStoreCount.set(0);
        totalOrderCount.set(0);
        successOrderCount.set(0);
        failOrderCount.set(0);
        syncingStores.clear();
        currentSyncingStoreCount.set(0);
        log.info("【批次控制】开始新的批量同步任务，总门店数: {}", totalStores);
        return true;
    }

    
    public synchronized boolean startBatchSync() {
        return startBatchSync(0);
    }

    
    public synchronized void finishBatchSync() {
        finishBatchSync(true);
    }

    
    public synchronized void finishBatchSync(boolean success) {
        long elapsed = System.currentTimeMillis() - batchSyncStartTime;
        int syncedCount = currentSyncingStoreCount.get();
        syncingBatch = false;
        syncStatus = success ? "COMPLETED" : "FAILED";
        batchSyncStartTime = 0;
        syncingStores.clear();
        currentSyncingStoreCount.set(0);
        log.info("【优雅关闭】批量同步完成，共同步{}家门店，耗时{}秒，状态: {}", 
                syncedCount, elapsed / 1000, syncStatus);
    }

    public synchronized boolean isBatchSyncing() {
        return syncingBatch;
    }

    
    public void registerStoreSyncStarted(String platformStoreId, String storeName) {
        String label = StrUtil.isNotBlank(storeName) ? storeName + "(" + platformStoreId + ")" : platformStoreId;
        syncingStores.add(label);
        currentSyncingStoreCount.incrementAndGet();
        log.debug("【同步跟踪】门店开始同步: {}, 当前同步数: {}", label, syncingStores.size());
    }

    
    public void registerStoreSyncFinished(String platformStoreId) {
        registerStoreSyncFinished(platformStoreId, true);
    }

    
    public void registerStoreSyncFinished(String platformStoreId, boolean success) {
        syncingStores.removeIf(s -> s.contains(platformStoreId));
        currentSyncingStoreCount.updateAndGet(count -> Math.max(0, count - 1));
        completedStoreCount.incrementAndGet();
        if (success) {
            successStoreCount.incrementAndGet();
        } else {
            failedStoreCount.incrementAndGet();
        }
        log.debug("【同步跟踪】门店同步完成: {}, 成功: {}, 当前同步数: {}", 
                platformStoreId, success, syncingStores.size());
    }

    
    public synchronized List<String> getCurrentSyncingStores() {
        return Collections.unmodifiableList(new ArrayList<>(syncingStores));
    }

    
    public int getCurrentSyncingStoreCount() {
        return currentSyncingStoreCount.get();
    }

    
    public long getBatchSyncStartTime() {
        return batchSyncStartTime;
    }

    
    public void triggerShutdown() {
        if (shuttingDown) {
            log.warn("【优雅关闭】已经触发过停机，忽略重复请求");
            return;
        }
        shuttingDown = true;
        log.info("【优雅关闭】触发优雅停止，当前活跃任务数={}", activeTasks.get());
    }

    
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    
    public int getActiveTaskCount() {
        return activeTasks.get();
    }

    
    public String getCurrentProcessingOrderId() {
        return currentProcessingOrderId;
    }

    
    public boolean waitForTasks(long timeoutMs) {
        long start = System.currentTimeMillis();
        int lastCount = activeTasks.get();

        while (activeTasks.get() > 0) {
            int currentCount = activeTasks.get();
            if (currentCount != lastCount) {
                log.info("【优雅关闭】等待任务完成，剩余活跃任务数={}", currentCount);
                lastCount = currentCount;
            }

            if (System.currentTimeMillis() - start > timeoutMs) {
                log.warn("【优雅关闭】等待任务超时，剩余活跃任务数={}", activeTasks.get());
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("【优雅关闭】等待任务被中断");
                return false;
            }
        }

        log.info("【优雅关闭】所有任务已完成");
        return true;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public int getTotalStoreCount() {
        return totalStoreCount;
    }

    public int getCompletedStoreCount() {
        return completedStoreCount.get();
    }

    public int getSuccessStoreCount() {
        return successStoreCount.get();
    }

    public int getFailedStoreCount() {
        return failedStoreCount.get();
    }

    
    public void addOrderCounts(int syncCount, int successCount, int failCount) {
        totalOrderCount.addAndGet(syncCount);
        successOrderCount.addAndGet(successCount);
        failOrderCount.addAndGet(failCount);
        log.debug("【同步跟踪】累加订单数: 总={}, 成功={}, 失败={}", syncCount, successCount, failCount);
    }

    public int getTotalOrderCount() {
        return totalOrderCount.get();
    }

    public int getSuccessOrderCount() {
        return successOrderCount.get();
    }

    public int getFailOrderCount() {
        return failOrderCount.get();
    }

    
    public String getStatusInfo() {
        return String.format(
                "shuttingDown=%s, activeTasks=%d, currentOrderId=%s, activeBatchSyncCount=%d",
                shuttingDown,
                activeTasks.get(),
                currentProcessingOrderId,
                activeBatchSyncCount.get());
    }
}
