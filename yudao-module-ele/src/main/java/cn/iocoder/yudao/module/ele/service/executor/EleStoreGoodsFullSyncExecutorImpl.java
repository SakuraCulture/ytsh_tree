package cn.iocoder.yudao.module.ele.service.executor;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncService;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    @Qualifier("eleStoreGoodsFullSyncExecutor")
    private ThreadPoolTaskExecutor fullSyncExecutor;

    @Override
    @Async
    public void submit(Long taskId) {
        execute(taskId);
    }

    @Override
    public void execute(Long taskId) {
        EleStoreGoodsFullSyncTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("【饿了么门店商品全量同步】任务不存在, taskId={}", taskId);
            return;
        }
        List<EleStoreGoodsFullSyncTaskStoreDO> taskStores = taskStoreMapper.selectListByTaskId(taskId);
        if (CollUtil.isEmpty(taskStores)) {
            markTaskFailed(taskId, "任务没有门店明细");
            return;
        }

        updateTaskRunning(taskId);
        forceRefreshTaskAggregate(taskId);
        int initialMaxInFlight = resolveMaxInFlight();
        List<CompletableFuture<Void>> inFlightFutures = new ArrayList<>(Math.min(taskStores.size(), initialMaxInFlight));
        for (EleStoreGoodsFullSyncTaskStoreDO taskStore : taskStores) {
            if (isCancelled(taskId)) {
                break;
            }
            waitForAvailableSlot(inFlightFutures, resolveMaxInFlight());
            try {
                inFlightFutures.add(CompletableFuture.runAsync(() -> executeStoreTask(task, taskStore), fullSyncExecutor));
            } catch (Exception ex) {
                log.error("【饿了么门店商品全量同步】提交门店任务失败, taskId={}, storeId={}", taskId, taskStore.getStoreId(), ex);
                StoreSyncSummary summary = new StoreSyncSummary();
                summary.success = false;
                summary.errorMsg = ex.getMessage();
                finishStore(taskStore.getId(), STATUS_FAILED, ex.getMessage(), summary);
                forceRefreshTaskAggregate(taskId);
            }
        }
        waitForAll(inFlightFutures);
        if (!isCancelled(taskId)) {
            finishTask(taskId);
        } else {
            forceRefreshTaskAggregate(taskId);
        }
        clearAggregateRefreshState(taskId);
    }

    private void executeStoreTask(EleStoreGoodsFullSyncTaskDO task, EleStoreGoodsFullSyncTaskStoreDO taskStore) {
        try {
            syncStore(task, taskStore);
        } finally {
            forceRefreshTaskAggregate(task.getId());
        }
    }

    private StoreSyncSummary syncStore(EleStoreGoodsFullSyncTaskDO task, EleStoreGoodsFullSyncTaskStoreDO taskStore) {
        StoreSyncSummary summary = new StoreSyncSummary();
        updateStoreRunning(taskStore.getId());
        try {
            int pageNo = 1;
            int pageSize = taskStore.getPageSize() == null || taskStore.getPageSize() < 1 ? 20 : taskStore.getPageSize();
            int totalPage = 1;
            do {
                if (isCancelled(task.getId())) {
                    finishStore(taskStore.getId(), STATUS_CANCELLED, null, summary);
                    return summary;
                }
                EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO();
                reqBO.setMerchantCode(taskStore.getMerchantCode());
                reqBO.setErpStoreCode(taskStore.getErpStoreCode());
                reqBO.setPageNo(pageNo);
                reqBO.setPageSize(pageSize);
                EleStoreGoodsPageSyncResult pageResult = syncService.syncStoreGoodsPage(reqBO, task.getTestMode());
                totalPage = calculateTotalPage(pageResult.getTotal(), pageResult.getPageSize());
                summary.totalPage = totalPage;
                summary.finishedPage = pageNo;
                summary.totalSkuCount += value(pageResult.getSyncCount());
                summary.successCount += value(pageResult.getSuccessCount());
                summary.failCount += value(pageResult.getFailCount());
                summary.governanceCount += value(pageResult.getGovernanceCount());
                updateStoreProgress(taskStore.getId(), pageNo, totalPage, pageSize, summary);
                refreshTaskAggregateIfNeeded(task.getId());
                pageNo++;
            } while (pageNo <= totalPage);
            finishStore(taskStore.getId(), STATUS_SUCCESS, null, summary);
            summary.success = true;
        } catch (Exception ex) {
            summary.success = false;
            summary.errorMsg = ex.getMessage();
            finishStore(taskStore.getId(), STATUS_FAILED, ex.getMessage(), summary);
        }
        return summary;
    }

    private int calculateTotalPage(Integer total, Integer pageSize) {
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        int normalizedTotal = total == null ? 0 : total;
        return Math.max(1, (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize);
    }

    private int resolveMaxInFlight() {
        int configuredMaxPoolSize = fullSyncExecutor.getMaxPoolSize();
        if (configuredMaxPoolSize > 0) {
            return configuredMaxPoolSize;
        }
        int configuredCorePoolSize = fullSyncExecutor.getCorePoolSize();
        if (configuredCorePoolSize > 0) {
            return configuredCorePoolSize;
        }
        return 1;
    }

    private void waitForAvailableSlot(List<CompletableFuture<Void>> inFlightFutures, int maxInFlight) {
        while (inFlightFutures.size() >= maxInFlight) {
            CompletableFuture.anyOf(inFlightFutures.toArray(new CompletableFuture[0])).join();
            inFlightFutures.removeIf(CompletableFuture::isDone);
        }
    }

    private void waitForAll(List<CompletableFuture<Void>> inFlightFutures) {
        while (!inFlightFutures.isEmpty()) {
            CompletableFuture.anyOf(inFlightFutures.toArray(new CompletableFuture[0])).join();
            inFlightFutures.removeIf(CompletableFuture::isDone);
        }
    }

    private boolean isCancelled(Long taskId) {
        EleStoreGoodsFullSyncTaskDO task = taskMapper.selectById(taskId);
        return task != null && STATUS_CANCELLED.equals(task.getStatus());
    }

    private void updateTaskRunning(Long taskId) {
        EleStoreGoodsFullSyncTaskDO updateObj = new EleStoreGoodsFullSyncTaskDO();
        updateObj.setId(taskId);
        updateObj.setStatus(STATUS_RUNNING);
        updateObj.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(updateObj);
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        EleStoreGoodsFullSyncTaskDO updateObj = new EleStoreGoodsFullSyncTaskDO();
        updateObj.setId(taskId);
        updateObj.setStatus(STATUS_FAILED);
        updateObj.setErrorMsg(errorMsg);
        updateObj.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(updateObj);
    }

    private void finishTask(Long taskId) {
        TaskAggregate aggregate = buildAggregate(taskId);
        EleStoreGoodsFullSyncTaskDO updateObj = new EleStoreGoodsFullSyncTaskDO();
        updateObj.setId(taskId);
        updateObj.setStatus(aggregate.failedStoreCount == 0 ? STATUS_SUCCESS : STATUS_PARTIAL_FAIL);
        updateObj.setFinishedStoreCount(aggregate.finishedStoreCount);
        updateObj.setTotalPageCount(aggregate.totalPageCount);
        updateObj.setFinishedPageCount(aggregate.finishedPageCount);
        updateObj.setTotalSkuCount(aggregate.totalSkuCount);
        updateObj.setSuccessCount(aggregate.successCount);
        updateObj.setFailCount(aggregate.failCount);
        updateObj.setGovernanceCount(aggregate.governanceCount);
        updateObj.setErrorMsg(aggregate.joinedErrorMsg());
        updateObj.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(updateObj);
    }

    private void updateStoreRunning(Long taskStoreId) {
        EleStoreGoodsFullSyncTaskStoreDO updateObj = new EleStoreGoodsFullSyncTaskStoreDO();
        updateObj.setId(taskStoreId);
        updateObj.setStatus(STATUS_RUNNING);
        updateObj.setStartedAt(LocalDateTime.now());
        taskStoreMapper.updateById(updateObj);
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
        taskStoreMapper.updateById(updateObj);
    }

    private void finishStore(Long taskStoreId, String status, String errorMsg, StoreSyncSummary summary) {
        EleStoreGoodsFullSyncTaskStoreDO updateObj = new EleStoreGoodsFullSyncTaskStoreDO();
        updateObj.setId(taskStoreId);
        updateObj.setStatus(status);
        updateObj.setCurrentPage(summary.finishedPage);
        updateObj.setTotalPage(summary.totalPage);
        updateObj.setTotalSkuCount(summary.totalSkuCount);
        updateObj.setSuccessCount(summary.successCount);
        updateObj.setFailCount(summary.failCount);
        updateObj.setGovernanceCount(summary.governanceCount);
        updateObj.setErrorMsg(errorMsg);
        updateObj.setFinishedAt(LocalDateTime.now());
        taskStoreMapper.updateById(updateObj);
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
        taskMapper.updateById(updateObj);
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
        private boolean success;
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
