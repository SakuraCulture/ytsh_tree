package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 优雅关闭状态管理器
 *
 * 用于管理应用优雅关闭过程中的状态，确保正在执行的任务能够安全完成，
 * 避免在订单同步过程中产生脏数据。
 *
 * @author 优团科技数字化团队
 */
@Slf4j
@Component
public class ShutdownStateManager {

    /**
     * 全局停机状态标志
     */
    private volatile boolean shuttingDown = false;

    /**
     * 正在执行的任务数
     */
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * 记录当前正在处理的订单ID（用于调试和日志）
     */
    private volatile String currentProcessingOrderId;

    /**
     * 正在执行批量同步的门店数
     */
    private final AtomicInteger activeBatchSyncCount = new AtomicInteger(0);

    /**
     * 标记是否正在处理批量同步任务
     */
    private volatile boolean syncingBatch = false;

    /**
     * 当前正在同步的门店列表
     */
    private final List<String> syncingStores = new CopyOnWriteArrayList<>();

    /**
     * 当前正在同步的门店数量（实时）
     */
    private final AtomicInteger currentSyncingStoreCount = new AtomicInteger(0);

    /**
     * 批次同步开始时间（毫秒时间戳）
     */
    private volatile long batchSyncStartTime = 0;

    /**
     * 注册任务开始
     *
     * @param orderId 当前处理的订单ID
     */
    public void taskStarted(String orderId) {
        activeTasks.incrementAndGet();
        currentProcessingOrderId = orderId;
        log.debug("【优雅关闭】任务开始，orderId={}, 当前活跃任务数={}", orderId, activeTasks.get());
    }

    /**
     * 注册任务结束
     */
    public void taskFinished() {
        activeTasks.decrementAndGet();
        if (activeTasks.get() == 0) {
            currentProcessingOrderId = null;
        }
        log.debug("【优雅关闭】任务结束，当前活跃任务数={}", activeTasks.get());
    }

    /**
     * 检查是否可以接受新任务
     *
     * @return true-可以接受新任务，false-正在关闭，不应接受新任务
     */
    public boolean canAcceptNewTask() {
        return !shuttingDown;
    }

    /**
     * 检查是否可以接受新的批量同步任务
     *
     * @return true-可以接受新批次，false-正在关闭或已有批次在执行
     */
    public boolean canAcceptNewBatch() {
        return !shuttingDown && activeBatchSyncCount.get() == 0;
    }

    /**
     * 标记开始处理批量同步
     *
     * @return true-成功标记，false-正在关闭，无法开始新批次
     */
    public synchronized boolean startBatchSync() {
        if (shuttingDown) {
            log.warn("【优雅关闭】应用正在关闭，拒绝新的批量同步任务");
            return false;
        }
        if (syncingBatch) {
            log.warn("【批次控制】已有批次在执行，拒绝新的批量同步任务");
            return false;
        }
        syncingBatch = true;
        batchSyncStartTime = System.currentTimeMillis();
        syncingStores.clear();
        currentSyncingStoreCount.set(0);
        log.info("【批次控制】开始新的批量同步任务");
        return true;
    }

    public synchronized void finishBatchSync() {
        long elapsed = System.currentTimeMillis() - batchSyncStartTime;
        int syncedCount = currentSyncingStoreCount.get();
        syncingBatch = false;
        batchSyncStartTime = 0;
        syncingStores.clear();
        currentSyncingStoreCount.set(0);
        log.info("【优雅关闭】批量同步完成，共同步{}家门店，耗时{}秒", syncedCount, elapsed / 1000);
    }

    public synchronized boolean isBatchSyncing() {
        return syncingBatch;
    }

    /**
     * 注册门店开始同步
     *
     * @param platformStoreId 平台门店ID
     * @param storeName 门店名称
     */
    public void registerStoreSyncStarted(String platformStoreId, String storeName) {
        String label = StrUtil.isNotBlank(storeName) ? storeName + "(" + platformStoreId + ")" : platformStoreId;
        syncingStores.add(label);
        currentSyncingStoreCount.incrementAndGet();
        log.debug("【同步跟踪】门店开始同步: {}, 当前同步数: {}", label, syncingStores.size());
    }

    /**
     * 注册门店同步完成
     *
     * @param platformStoreId 平台门店ID
     */
    public void registerStoreSyncFinished(String platformStoreId) {
        syncingStores.removeIf(s -> s.contains(platformStoreId));
        currentSyncingStoreCount.decrementAndGet();
        log.debug("【同步跟踪】门店同步完成: {}, 当前同步数: {}", platformStoreId, syncingStores.size());
    }

    /**
     * 获取当前正在同步的门店列表
     *
     * @return 门店名称/ID列表
     */
    public synchronized List<String> getCurrentSyncingStores() {
        return Collections.unmodifiableList(new ArrayList<>(syncingStores));
    }

    /**
     * 获取当前同步的门店数量
     *
     * @return 门店数量
     */
    public int getCurrentSyncingStoreCount() {
        return currentSyncingStoreCount.get();
    }

    /**
     * 获取批次同步开始时间
     *
     * @return 毫秒时间戳，0表示未开始
     */
    public long getBatchSyncStartTime() {
        return batchSyncStartTime;
    }

    /**
     * 触发停机
     */
    public void triggerShutdown() {
        if (shuttingDown) {
            log.warn("【优雅关闭】已经触发过停机，忽略重复请求");
            return;
        }
        shuttingDown = true;
        log.info("【优雅关闭】触发优雅停止，当前活跃任务数={}", activeTasks.get());
    }

    /**
     * 检查是否正在关闭
     *
     * @return true-正在关闭，false-正常运行
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * 获取当前活跃任务数
     *
     * @return 活跃任务数
     */
    public int getActiveTaskCount() {
        return activeTasks.get();
    }

    /**
     * 获取当前正在处理的订单ID
     *
     * @return 订单ID
     */
    public String getCurrentProcessingOrderId() {
        return currentProcessingOrderId;
    }

    /**
     * 等待所有任务完成
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return true-所有任务在规定时间内完成，false-超时
     */
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

    /**
     * 获取详细的停机状态信息
     *
     * @return 状态信息
     */
    public String getStatusInfo() {
        return String.format(
                "shuttingDown=%s, activeTasks=%d, currentOrderId=%s, activeBatchSyncCount=%d",
                shuttingDown,
                activeTasks.get(),
                currentProcessingOrderId,
                activeBatchSyncCount.get());
    }
}
