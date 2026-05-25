package cn.iocoder.yudao.module.ele.service.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncService;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.guard.StoreExecutionGuard;
import cn.iocoder.yudao.module.ele.service.guard.StoreExecutionScenario;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class EleStoreGoodsFullSyncExecutorImpl implements EleStoreGoodsFullSyncExecutor {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PARTIAL_FAIL = "PARTIAL_FAIL";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final long AGGREGATE_REFRESH_INTERVAL_MILLIS = 1000L;

    private final ConcurrentMap<Long, Long> aggregateRefreshAtMap = new ConcurrentHashMap<>();

    @Resource
    private EleStoreGoodsFullSyncTaskMapper taskMapper;
    @Resource
    private EleStoreGoodsFullSyncTaskStoreMapper taskStoreMapper;
    @Resource
    private EleStoreGoodsSyncService syncService;
    @Resource
    @Qualifier("eleStoreGoodsPageExecutor")
    private ThreadPoolExecutor pageExecutor;
    @Resource
    private StoreExecutionGuard storeExecutionGuard;

    @Override
    @Async
    public void submit(Long taskId) {
        execute(taskId);
    }

    @Override
    @Async
    public void submitDirectly(List<StorePlatformRespVO> stores, Boolean testMode) {
        executeDirectly(stores, testMode);
    }

    @Override
    public void executeDirectly(List<StorePlatformRespVO> stores, Boolean testMode) {
        long taskStartTime = System.currentTimeMillis();
        log.info("【饿了么门店商品全量同步】========== 直接执行开始 ==========");
        log.info("【饿了么门店商品全量同步】门店总数={}, testMode={}", stores.size(), testMode);

        AtomicInteger finishedStoreCount = new AtomicInteger(0);
        AtomicInteger failedStoreCount = new AtomicInteger(0);
        AtomicInteger totalSkuCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger governanceCount = new AtomicInteger(0);

        for (StorePlatformRespVO store : stores) {
            String storeName = StrUtil.blankToDefault(store.getPlatformStoreName(), store.getPlatformStoreId());
            String merchantCode = StrUtil.trim(store.getSettlementAccount());
            String erpStoreCode = StrUtil.trim(store.getPlatformStoreId());
            long storeStartTime = System.currentTimeMillis();
            log.info("【饿了么门店商品全量同步】开始同步门店: store={}", storeName);
            try {
                storeExecutionGuard.runWithStoreLock(StoreExecutionScenario.STORE_GOODS,
                        store.getPlatformStoreId(), () -> {
                            int pageNo = 1;
                            int pageSize = 20;
                            int totalPage = 1;
                            do {
                                EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO();
                                reqBO.setMerchantCode(merchantCode);
                                reqBO.setErpStoreCode(erpStoreCode);
                                reqBO.setPageNo(pageNo);
                                reqBO.setPageSize(pageSize);
                                EleStoreGoodsPageSyncResult pageResult = syncService.syncStoreGoodsPage(reqBO, testMode);
                                totalPage = calculateTotalPage(pageResult.getTotal(), pageResult.getPageSize());
                                totalSkuCount.addAndGet(value(pageResult.getSyncCount()));
                                successCount.addAndGet(value(pageResult.getSuccessCount()));
                                failCount.addAndGet(value(pageResult.getFailCount()));
                                governanceCount.addAndGet(value(pageResult.getGovernanceCount()));
                                log.info("【饿了么门店商品全量同步】门店分页同步: store={}, 第{}/{}页, 本页SKU={}, 成功={}, 失败={}, 影子={}",
                                        storeName, pageNo, totalPage, pageResult.getSyncCount(),
                                        pageResult.getSuccessCount(), pageResult.getFailCount(),
                                        pageResult.getShadowCount());
                                pageNo++;
                            } while (pageNo <= totalPage);
                        });
                finishedStoreCount.incrementAndGet();
                long duration = System.currentTimeMillis() - storeStartTime;
                log.info("【饿了么门店商品全量同步】门店完成: store={}, 耗时={}ms", storeName, duration);
            } catch (Exception ex) {
                failedStoreCount.incrementAndGet();
                long duration = System.currentTimeMillis() - storeStartTime;
                log.error("【饿了么门店商品全量同步】门店失败: store={}, 耗时={}ms, 错误={}",
                        storeName, duration, ex.getMessage());
            }
        }

        long totalDuration = System.currentTimeMillis() - taskStartTime;
        log.info("【饿了么门店商品全量同步】========== 直接执行完成 ==========");
        log.info("【饿了么门店商品全量同步】完成门店={}/{}, 失败={}, SKU总数={}, 成功={}, 失败={}, 治理={}, 总耗时={}ms",
                finishedStoreCount.get(), stores.size(), failedStoreCount.get(),
                totalSkuCount.get(), successCount.get(), failCount.get(), governanceCount.get(), totalDuration);
    }

    @Override
    public void execute(Long taskId) {
        EleStoreGoodsFullSyncTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("【饿了么门店商品全量同步】任务不存在, taskId={}", taskId);
            return;
        }
        if (!claimTask(taskId)) {
            log.info("【饿了么门店商品全量同步】任务已被其他执行流领取, taskId={}", taskId);
            return;
        }
        List<EleStoreGoodsFullSyncTaskStoreDO> taskStores = taskStoreMapper.selectListByTaskId(taskId);
        if (CollUtil.isEmpty(taskStores)) {
            markTaskFailed(taskId, "任务没有门店明细");
            return;
        }

        long taskStartTime = System.currentTimeMillis();
        int pagePoolSize = pageExecutor.getMaximumPoolSize();
        log.info("【饿了么门店商品全量同步】========== 任务开始执行 ==========");
        log.info("【饿了么门店商品全量同步】taskId={}, scope={}, 门店总数={}, 页并发度={}",
                taskId, task.getScope(), taskStores.size(), pagePoolSize);

        forceRefreshTaskAggregate(taskId);

        for (EleStoreGoodsFullSyncTaskStoreDO taskStore : taskStores) {
            if (isCancelled(taskId)) {
                log.info("【饿了么门店商品全量同步】任务已取消, 停止执行, taskId={}", taskId);
                break;
            }
            executeStoreTask(task, taskStore);
            forceRefreshTaskAggregate(taskId);
        }

        if (!isCancelled(taskId)) {
            finishTask(taskId);
            long duration = System.currentTimeMillis() - taskStartTime;
            TaskAggregate aggregate = buildAggregate(taskId);
            log.info("【饿了么门店商品全量同步】========== 任务执行完成 ==========");
            log.info("【饿了么门店商品全量同步】taskId={}, 完成门店={}/{}, 总页数={}, SKU总数={}, 成功={}, 失败={}, 治理={}, 总耗时={}ms",
                    taskId, aggregate.finishedStoreCount, taskStores.size(), aggregate.totalPageCount,
                    aggregate.totalSkuCount, aggregate.successCount, aggregate.failCount,
                    aggregate.governanceCount, duration);
        } else {
            forceRefreshTaskAggregate(taskId);
            log.info("【饿了么门店商品全量同步】任务已取消, taskId={}", taskId);
        }
        clearAggregateRefreshState(taskId);
    }

    private void executeStoreTask(EleStoreGoodsFullSyncTaskDO task, EleStoreGoodsFullSyncTaskStoreDO taskStore) {
        String storeName = taskStore.getStoreName() != null ? taskStore.getStoreName() : taskStore.getPlatformStoreId();
        StoreSyncSummary summary = new StoreSyncSummary();
        if (!updateStoreRunning(taskStore.getId())) {
            log.info("【饿了么门店商品全量同步】门店明细已被恢复流程接管, 跳过执行, store={}", storeName);
            return;
        }
        long storeStartTime = System.currentTimeMillis();
        log.info("【饿了么门店商品全量同步】开始同步门店: store={}", storeName);
        try {
            storeExecutionGuard.runWithStoreLock(StoreExecutionScenario.STORE_GOODS,
                    taskStore.getPlatformStoreId(), () -> syncStorePages(task, taskStore, summary));
            if (!summary.cancelled) {
                finishStore(taskStore.getId(), STATUS_SUCCESS, null, summary);
                long duration = System.currentTimeMillis() - storeStartTime;
                log.info("【饿了么门店商品全量同步】门店完成: store={}, 商品总数={}, 写入数据库={}, 成功={}, 失败={}, 治理={}, 页数={}/{}, 耗时={}ms",
                        storeName, summary.totalSkuCount, summary.successCount + summary.governanceCount,
                        summary.successCount, summary.failCount, summary.governanceCount,
                        summary.finishedPage, summary.totalPage, duration);
            }
        } catch (Exception ex) {
            summary.errorMsg = ex.getMessage();
            finishStore(taskStore.getId(), STATUS_FAILED, ex.getMessage(), summary);
            long duration = System.currentTimeMillis() - storeStartTime;
            log.error("【饿了么门店商品全量同步】门店失败: store={}, 商品总数={}, 写入数据库={}, 耗时={}ms, 错误={}",
                    storeName, summary.totalSkuCount, summary.successCount + summary.governanceCount,
                    duration, ex.getMessage());
        }
    }

    private void syncStorePages(EleStoreGoodsFullSyncTaskDO task, EleStoreGoodsFullSyncTaskStoreDO taskStore,
                                StoreSyncSummary summary) {
        int pageSize = taskStore.getPageSize() == null || taskStore.getPageSize() < 1 ? 20 : taskStore.getPageSize();

        EleStoreGoodsPageSyncResult firstResult = syncSinglePage(task, taskStore, 1, pageSize);
        int totalPage = calculateTotalPage(firstResult.getTotal(), firstResult.getPageSize());
        summary.totalPage = totalPage;
        summary.finishedPage = 1;
        summary.totalSkuCount += value(firstResult.getSyncCount());
        summary.successCount += value(firstResult.getSuccessCount());
        summary.failCount += value(firstResult.getFailCount());
        summary.governanceCount += value(firstResult.getGovernanceCount());

        if (totalPage <= 1 || isCancelled(task.getId())) {
            if (isCancelled(task.getId())) {
                summary.cancelled = true;
                finishStore(taskStore.getId(), STATUS_CANCELLED, null, summary);
            }
            return;
        }

        int maxConcurrent = pageExecutor.getMaximumPoolSize();
        AtomicInteger completedPages = new AtomicInteger(1);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int pageNo = 2; pageNo <= totalPage; pageNo++) {
            if (isCancelled(task.getId())) {
                summary.cancelled = true;
                finishStore(taskStore.getId(), STATUS_CANCELLED, null, summary);
                break;
            }
            final int currentPage = pageNo;
            while (futures.size() >= maxConcurrent) {
                CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
                futures.removeIf(CompletableFuture::isDone);
            }
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    EleStoreGoodsPageSyncResult pageResult = syncSinglePage(task, taskStore, currentPage, pageSize);
                    synchronized (summary) {
                        summary.totalSkuCount += value(pageResult.getSyncCount());
                        summary.successCount += value(pageResult.getSuccessCount());
                        summary.failCount += value(pageResult.getFailCount());
                        summary.governanceCount += value(pageResult.getGovernanceCount());
                        summary.finishedPage = completedPages.incrementAndGet();
                    }
                    updateStoreProgress(taskStore.getId(), completedPages.get(), totalPage, pageSize, summary);
                    refreshTaskAggregateIfNeeded(task.getId());
                } catch (Exception ex) {
                    log.error("【饿了么门店商品全量同步】分页同步异常: store={}, page={}, err={}",
                            taskStore.getStoreName(), currentPage, ex.getMessage());
                    synchronized (summary) {
                        summary.failCount++;
                        summary.finishedPage = completedPages.incrementAndGet();
                    }
                }
            }, pageExecutor));
        }

        waitForAll(futures);
    }

    private EleStoreGoodsPageSyncResult syncSinglePage(EleStoreGoodsFullSyncTaskDO task,
                                                        EleStoreGoodsFullSyncTaskStoreDO taskStore,
                                                        int pageNo, int pageSize) {
        EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO();
        reqBO.setMerchantCode(taskStore.getMerchantCode());
        reqBO.setErpStoreCode(taskStore.getErpStoreCode());
        reqBO.setPageNo(pageNo);
        reqBO.setPageSize(pageSize);
        return syncService.syncStoreGoodsPage(reqBO, task.getTestMode());
    }

    private int calculateTotalPage(Integer total, Integer pageSize) {
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        int normalizedTotal = total == null ? 0 : total;
        return Math.max(1, (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize);
    }

    private void waitForAll(List<CompletableFuture<Void>> futures) {
        while (!futures.isEmpty()) {
            CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
            futures.removeIf(CompletableFuture::isDone);
        }
    }

    private boolean isCancelled(Long taskId) {
        EleStoreGoodsFullSyncTaskDO task = taskMapper.selectById(taskId);
        return task != null && STATUS_CANCELLED.equals(task.getStatus());
    }

    private boolean claimTask(Long taskId) {
        return taskMapper.markRunningIfPending(taskId, LocalDateTime.now()) > 0;
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        taskMapper.markFailedIfRunning(taskId, errorMsg, LocalDateTime.now());
    }

    private void finishTask(Long taskId) {
        TaskAggregate aggregate = buildAggregate(taskId);
        EleStoreGoodsFullSyncTaskDO updateObj = new EleStoreGoodsFullSyncTaskDO();
        updateObj.setId(taskId);
        updateObj.setFinishedStoreCount(aggregate.finishedStoreCount);
        updateObj.setTotalPageCount(aggregate.totalPageCount);
        updateObj.setFinishedPageCount(aggregate.finishedPageCount);
        updateObj.setTotalSkuCount(aggregate.totalSkuCount);
        updateObj.setSuccessCount(aggregate.successCount);
        updateObj.setFailCount(aggregate.failCount);
        updateObj.setGovernanceCount(aggregate.governanceCount);
        updateObj.setErrorMsg(aggregate.joinedErrorMsg());
        updateObj.setFinishedAt(LocalDateTime.now());
        taskMapper.finishIfRunning(taskId, aggregate.failedStoreCount == 0 ? STATUS_SUCCESS : STATUS_PARTIAL_FAIL, updateObj);
    }

    private boolean updateStoreRunning(Long taskStoreId) {
        return taskStoreMapper.markRunningIfPending(taskStoreId, LocalDateTime.now()) > 0;
    }

    private void updateStoreProgress(Long taskStoreId, int currentPage, int totalPage, int pageSize, StoreSyncSummary summary) {
        EleStoreGoodsFullSyncTaskStoreDO updateObj = new EleStoreGoodsFullSyncTaskStoreDO();
        updateObj.setId(taskStoreId);
        updateObj.setCurrentPage(currentPage);
        updateObj.setTotalPage(totalPage);
        updateObj.setPageSize(pageSize);
        updateObj.setTotalSkuCount(summary.totalSkuCount);
        updateObj.setSuccessCount(summary.successCount);
        updateObj.setFailCount(summary.failCount);
        updateObj.setGovernanceCount(summary.governanceCount);
        taskStoreMapper.updateProgressIfRunning(taskStoreId, updateObj);
    }

    private void finishStore(Long taskStoreId, String status, String errorMsg, StoreSyncSummary summary) {
        EleStoreGoodsFullSyncTaskStoreDO updateObj = new EleStoreGoodsFullSyncTaskStoreDO();
        updateObj.setId(taskStoreId);
        updateObj.setCurrentPage(summary.finishedPage);
        updateObj.setTotalPage(summary.totalPage);
        updateObj.setTotalSkuCount(summary.totalSkuCount);
        updateObj.setSuccessCount(summary.successCount);
        updateObj.setFailCount(summary.failCount);
        updateObj.setGovernanceCount(summary.governanceCount);
        updateObj.setErrorMsg(errorMsg);
        updateObj.setFinishedAt(LocalDateTime.now());
        taskStoreMapper.finishIfRunning(taskStoreId, status, updateObj);
    }

    private void refreshTaskAggregate(Long taskId) {
        TaskAggregate aggregate = buildAggregate(taskId);
        EleStoreGoodsFullSyncTaskDO updateObj = new EleStoreGoodsFullSyncTaskDO();
        updateObj.setId(taskId);
        updateObj.setFinishedStoreCount(aggregate.finishedStoreCount);
        updateObj.setTotalPageCount(aggregate.totalPageCount);
        updateObj.setFinishedPageCount(aggregate.finishedPageCount);
        updateObj.setTotalSkuCount(aggregate.totalSkuCount);
        updateObj.setSuccessCount(aggregate.successCount);
        updateObj.setFailCount(aggregate.failCount);
        updateObj.setGovernanceCount(aggregate.governanceCount);
        updateObj.setErrorMsg(aggregate.joinedErrorMsg());
        taskMapper.refreshAggregateIfRunning(taskId, updateObj);
        aggregateRefreshAtMap.put(taskId, System.currentTimeMillis());
    }

    private void refreshTaskAggregateIfNeeded(Long taskId) {
        Long lastRefreshAt = aggregateRefreshAtMap.get(taskId);
        long now = System.currentTimeMillis();
        if (lastRefreshAt != null && now - lastRefreshAt < AGGREGATE_REFRESH_INTERVAL_MILLIS) {
            return;
        }
        forceRefreshTaskAggregate(taskId);
    }

    private void forceRefreshTaskAggregate(Long taskId) {
        try {
            refreshTaskAggregate(taskId);
        } catch (Exception ex) {
            log.warn("【饿了么门店商品全量同步】刷新任务聚合失败, taskId={}", taskId, ex);
        }
    }

    private void clearAggregateRefreshState(Long taskId) {
        aggregateRefreshAtMap.remove(taskId);
    }

    private TaskAggregate buildAggregate(Long taskId) {
        List<EleStoreGoodsFullSyncTaskStoreDO> stores = taskStoreMapper.selectListByTaskId(taskId);
        TaskAggregate aggregate = new TaskAggregate();
        for (EleStoreGoodsFullSyncTaskStoreDO store : stores) {
            aggregate.add(store);
        }
        return aggregate;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static class StoreSyncSummary {
        private boolean cancelled;
        private int totalPage;
        private int finishedPage;
        private int totalSkuCount;
        private int successCount;
        private int failCount;
        private int governanceCount;
        private String errorMsg;
    }

    private static class TaskAggregate {
        private int finishedStoreCount;
        private int failedStoreCount;
        private int totalPageCount;
        private int finishedPageCount;
        private int totalSkuCount;
        private int successCount;
        private int failCount;
        private int governanceCount;
        private final List<String> errorMessages = new ArrayList<>();

        private void add(EleStoreGoodsFullSyncTaskStoreDO store) {
            String status = store.getStatus();
            if (STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status) || STATUS_PARTIAL_FAIL.equals(status)
                    || STATUS_CANCELLED.equals(status)) {
                finishedStoreCount++;
            }
            if (STATUS_FAILED.equals(status) || STATUS_PARTIAL_FAIL.equals(status)) {
                failedStoreCount++;
            }
            totalPageCount += valueOf(store.getTotalPage());
            finishedPageCount += valueOf(store.getCurrentPage());
            totalSkuCount += valueOf(store.getTotalSkuCount());
            successCount += valueOf(store.getSuccessCount());
            failCount += valueOf(store.getFailCount());
            governanceCount += valueOf(store.getGovernanceCount());
            if (store.getErrorMsg() != null && !store.getErrorMsg().isBlank()) {
                errorMessages.add(store.getStoreName() + ": " + store.getErrorMsg());
            }
        }

        private String joinedErrorMsg() {
            if (errorMessages.isEmpty()) {
                return null;
            }
            return String.join(" | ", errorMessages);
        }

        private int valueOf(Integer value) {
            return value == null ? 0 : value;
        }
    }
}
