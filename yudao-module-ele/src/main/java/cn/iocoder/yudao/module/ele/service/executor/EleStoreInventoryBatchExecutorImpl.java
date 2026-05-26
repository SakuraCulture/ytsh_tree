package cn.iocoder.yudao.module.ele.service.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleSkuInventoryQueryService;
import cn.iocoder.yudao.module.ele.service.EleStoreInventorySkuScopeService;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryBatchQueryReqBO;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;
import cn.iocoder.yudao.module.ele.service.guard.StoreExecutionGuard;
import cn.iocoder.yudao.module.ele.service.guard.StoreExecutionScenario;
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

@Slf4j
@Component
public class EleStoreInventoryBatchExecutorImpl implements EleStoreInventoryBatchExecutor {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PARTIAL_FAIL = "PARTIAL_FAIL";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int SKU_BATCH_SIZE = 50;
    private static final int MAX_ERROR_MSG_LENGTH = 4000;

    @Resource
    private EleStoreInventoryBatchTaskMapper taskMapper;
    @Resource
    private EleStoreInventoryBatchTaskStoreMapper taskStoreMapper;
    @Resource
    private EleSkuInventoryQueryService queryService;
    @Resource
    private EleStoreInventorySkuScopeService skuScopeService;
    @Resource
    private StoreExecutionGuard storeExecutionGuard;
    @Resource
    @Qualifier("eleStoreInventoryBatchExecutor")
    private ThreadPoolTaskExecutor batchExecutor;

    @Override
    @Async
    public void submit(Long taskId) {
        execute(taskId);
    }

    @Override
    public void execute(Long taskId) {
        EleStoreInventoryBatchTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("【饿了么门店库存批量任务】任务不存在, taskId={}", taskId);
            return;
        }
        if (!claimTask(taskId)) {
            log.info("【饿了么门店库存批量任务】任务已被其他执行流领取, taskId={}", taskId);
            return;
        }
        List<EleStoreInventoryBatchTaskStoreDO> taskStores = taskStoreMapper.selectListByTaskId(taskId);
        if (CollUtil.isEmpty(taskStores)) {
            markTaskFailed(taskId, "任务没有门店明细");
            return;
        }

        forceRefreshTaskAggregate(taskId);
        int initialMaxInFlight = resolveMaxInFlight();
        List<CompletableFuture<Void>> inFlightFutures = new ArrayList<>(Math.min(taskStores.size(), initialMaxInFlight));
        for (EleStoreInventoryBatchTaskStoreDO taskStore : taskStores) {
            if (isCancelled(taskId)) {
                break;
            }
            waitForAvailableSlot(inFlightFutures, resolveMaxInFlight());
            try {
                inFlightFutures.add(CompletableFuture.runAsync(() -> executeStoreTask(task, taskStore), batchExecutor));
            } catch (Exception ex) {
                log.error("【饿了么门店库存批量任务】提交门店任务失败, taskId={}, storeId={}", taskId, taskStore.getStoreId(), ex);
                StoreTaskSummary summary = new StoreTaskSummary();
                summary.failureCount = 1;
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
    }

    private void executeStoreTask(EleStoreInventoryBatchTaskDO task, EleStoreInventoryBatchTaskStoreDO taskStore) {
        StoreTaskSummary summary = new StoreTaskSummary();
        String errorMsg = null;
        try {
            if (!updateStoreRunning(taskStore.getId())) {
                log.info("【饿了么门店库存批量任务】门店明细已被恢复流程接管, 跳过执行, taskStoreId={}", taskStore.getId());
                return;
            }
            if (isCancelled(task.getId())) {
                finishStore(taskStore.getId(), STATUS_CANCELLED, null, summary);
                return;
            }

            storeExecutionGuard.runWithStoreLock(StoreExecutionScenario.STORE_INVENTORY,
                    taskStore.getPlatformStoreId(), () -> executeStoreBatches(task, taskStore, summary));
            errorMsg = summary.errorMsg;
            finishStore(taskStore.getId(), resolveStoreStatus(summary, errorMsg), errorMsg, summary);
        } catch (TaskCancelledException ex) {
            finishStore(taskStore.getId(), STATUS_CANCELLED, summary.errorMsg, summary);
        } catch (Exception ex) {
            log.error("【饿了么门店库存批量任务】门店任务执行失败, taskId={}, storeId={}", task.getId(), taskStore.getStoreId(), ex);
            summary.failureCount++;
            finishStore(taskStore.getId(), STATUS_FAILED, ex.getMessage(), summary);
        } finally {
            forceRefreshTaskAggregate(task.getId());
        }
    }

    private void executeStoreBatches(EleStoreInventoryBatchTaskDO task,
                                     EleStoreInventoryBatchTaskStoreDO taskStore,
                                     StoreTaskSummary summary) {
        List<String> skuCodes = skuScopeService.listStoreSkuScope(taskStore.getStoreId(), taskStore.getErpStoreCode());
        if (CollUtil.isEmpty(skuCodes)) {
            return;
        }

        List<List<String>> skuBatches = CollUtil.split(skuCodes, SKU_BATCH_SIZE);
        summary.totalBatchNo = skuBatches.size();
        for (int i = 0; i < skuBatches.size(); i++) {
            if (isCancelled(task.getId())) {
                throw new TaskCancelledException();
            }
            List<String> batch = skuBatches.get(i);
            EleSkuInventoryBatchQueryRespDTO respDTO = queryStoreInventory(taskStore, batch);
            summary.finishedBatchNo = i + 1;
            summary.totalSkuCount += batch.size();
            summary.formalSuccessCount += valueOf(respDTO.getFormalSuccessCount());
            summary.shadowSuccessCount += valueOf(respDTO.getShadowSuccessCount());
            summary.governanceCount += valueOf(respDTO.getGovernanceCount());
            summary.failureCount += valueOf(respDTO.getFailureCount());
            summary.errorMsg = appendError(summary.errorMsg, joinErrors(respDTO.getErrorDetails()));
            updateStoreProgress(taskStore.getId(), summary);
        }
    }

    private EleSkuInventoryBatchQueryRespDTO queryStoreInventory(EleStoreInventoryBatchTaskStoreDO taskStore,
                                                                 List<String> skuCodes) {
        EleSkuInventoryBatchQueryReqBO reqBO = new EleSkuInventoryBatchQueryReqBO();
        reqBO.setPlatformStoreId(taskStore.getPlatformStoreId());
        reqBO.setStoreId(taskStore.getStoreId());
        reqBO.setMerchantCode(taskStore.getMerchantCode());
        reqBO.setErpStoreCode(taskStore.getErpStoreCode());
        reqBO.setSkuCodes(skuCodes);
        return queryService.queryBatch(reqBO);
    }

    private String resolveStoreStatus(StoreTaskSummary summary, String errorMsg) {
        boolean hasFailure = summary.failureCount > 0 || StrUtil.isNotBlank(errorMsg);
        boolean hasSuccess = summary.formalSuccessCount > 0 || summary.shadowSuccessCount > 0 || summary.totalSkuCount == 0;
        if (!hasFailure) {
            return STATUS_SUCCESS;
        }
        return hasSuccess ? STATUS_PARTIAL_FAIL : STATUS_FAILED;
    }

    private void updateStoreProgress(Long taskStoreId, StoreTaskSummary summary) {
        EleStoreInventoryBatchTaskStoreDO updateObj = new EleStoreInventoryBatchTaskStoreDO();
        updateObj.setId(taskStoreId);
        updateObj.setCurrentBatchNo(summary.finishedBatchNo);
        updateObj.setTotalBatchNo(summary.totalBatchNo);
        updateObj.setTotalSkuCount(summary.totalSkuCount);
        updateObj.setFormalSuccessCount(summary.formalSuccessCount);
        updateObj.setShadowSuccessCount(summary.shadowSuccessCount);
        updateObj.setGovernanceCount(summary.governanceCount);
        updateObj.setFailureCount(summary.failureCount);
        taskStoreMapper.updateProgressIfRunning(taskStoreId, updateObj);
    }

    private int resolveMaxInFlight() {
        int configuredMaxPoolSize = batchExecutor.getMaxPoolSize();
        if (configuredMaxPoolSize > 0) {
            return configuredMaxPoolSize;
        }
        int configuredCorePoolSize = batchExecutor.getCorePoolSize();
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
        EleStoreInventoryBatchTaskDO task = taskMapper.selectById(taskId);
        return task != null && STATUS_CANCELLED.equals(task.getStatus());
    }

    private boolean claimTask(Long taskId) {
        return taskMapper.markRunningIfPending(taskId, LocalDateTime.now()) > 0;
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        taskMapper.markFailedIfRunning(taskId, truncateErrorMsg(errorMsg), LocalDateTime.now());
    }

    private void finishTask(Long taskId) {
        TaskAggregate aggregate = buildAggregate(taskId);
        EleStoreInventoryBatchTaskDO updateObj = new EleStoreInventoryBatchTaskDO();
        updateObj.setId(taskId);
        updateObj.setFinishedStoreCount(aggregate.finishedStoreCount);
        updateObj.setTotalBatchCount(aggregate.totalBatchCount);
        updateObj.setFinishedBatchCount(aggregate.finishedBatchCount);
        updateObj.setTotalSkuCount(aggregate.totalSkuCount);
        updateObj.setFormalSuccessCount(aggregate.formalSuccessCount);
        updateObj.setShadowSuccessCount(aggregate.shadowSuccessCount);
        updateObj.setGovernanceCount(aggregate.governanceCount);
        updateObj.setFailureCount(aggregate.failureCount);
        updateObj.setErrorMsg(truncateErrorMsg(aggregate.joinedErrorMsg()));
        updateObj.setFinishedAt(LocalDateTime.now());
        taskMapper.finishIfRunning(taskId, aggregate.failedStoreCount == 0 ? STATUS_SUCCESS : STATUS_PARTIAL_FAIL, updateObj);
    }

    private boolean updateStoreRunning(Long taskStoreId) {
        return taskStoreMapper.markRunningIfPending(taskStoreId, LocalDateTime.now()) > 0;
    }

    private void finishStore(Long taskStoreId, String status, String errorMsg, StoreTaskSummary summary) {
        EleStoreInventoryBatchTaskStoreDO updateObj = new EleStoreInventoryBatchTaskStoreDO();
        updateObj.setId(taskStoreId);
        updateObj.setCurrentBatchNo(summary.finishedBatchNo);
        updateObj.setTotalBatchNo(summary.totalBatchNo);
        updateObj.setTotalSkuCount(summary.totalSkuCount);
        updateObj.setFormalSuccessCount(summary.formalSuccessCount);
        updateObj.setShadowSuccessCount(summary.shadowSuccessCount);
        updateObj.setGovernanceCount(summary.governanceCount);
        updateObj.setFailureCount(summary.failureCount);
        updateObj.setErrorMsg(truncateErrorMsg(errorMsg));
        updateObj.setFinishedAt(LocalDateTime.now());
        taskStoreMapper.finishIfRunning(taskStoreId, status, updateObj);
    }

    private void refreshTaskAggregate(Long taskId) {
        TaskAggregate aggregate = buildAggregate(taskId);
        EleStoreInventoryBatchTaskDO updateObj = new EleStoreInventoryBatchTaskDO();
        updateObj.setId(taskId);
        updateObj.setFinishedStoreCount(aggregate.finishedStoreCount);
        updateObj.setTotalBatchCount(aggregate.totalBatchCount);
        updateObj.setFinishedBatchCount(aggregate.finishedBatchCount);
        updateObj.setTotalSkuCount(aggregate.totalSkuCount);
        updateObj.setFormalSuccessCount(aggregate.formalSuccessCount);
        updateObj.setShadowSuccessCount(aggregate.shadowSuccessCount);
        updateObj.setGovernanceCount(aggregate.governanceCount);
        updateObj.setFailureCount(aggregate.failureCount);
        updateObj.setErrorMsg(truncateErrorMsg(aggregate.joinedErrorMsg()));
        taskMapper.refreshAggregateIfRunning(taskId, updateObj);
    }

    private void forceRefreshTaskAggregate(Long taskId) {
        try {
            refreshTaskAggregate(taskId);
        } catch (Exception ex) {
            log.warn("【饿了么门店库存批量任务】刷新任务聚合失败, taskId={}", taskId, ex);
        }
    }

    private TaskAggregate buildAggregate(Long taskId) {
        List<EleStoreInventoryBatchTaskStoreDO> stores = taskStoreMapper.selectListByTaskId(taskId);
        TaskAggregate aggregate = new TaskAggregate();
        for (EleStoreInventoryBatchTaskStoreDO store : stores) {
            aggregate.add(store);
        }
        return aggregate;
    }

    private String joinErrors(List<String> errorDetails) {
        if (CollUtil.isEmpty(errorDetails)) {
            return null;
        }
        return String.join(" | ", errorDetails);
    }

    private String appendError(String current, String next) {
        if (StrUtil.isBlank(next)) {
            return current;
        }
        if (StrUtil.isBlank(current)) {
            return next;
        }
        return current + " | " + next;
    }

    private int valueOf(Integer value) {
        return value == null ? 0 : value;
    }

    private String truncateErrorMsg(String msg) {
        if (StrUtil.isBlank(msg) || msg.length() <= MAX_ERROR_MSG_LENGTH) {
            return msg;
        }
        return msg.substring(0, MAX_ERROR_MSG_LENGTH) + "...(truncated)";
    }

    private static class StoreTaskSummary {
        private int totalBatchNo;
        private int finishedBatchNo;
        private int totalSkuCount;
        private int formalSuccessCount;
        private int shadowSuccessCount;
        private int governanceCount;
        private int failureCount;
        private String errorMsg;
    }

    private static class TaskCancelledException extends RuntimeException {
    }

    private static class TaskAggregate {
        private int finishedStoreCount;
        private int failedStoreCount;
        private int totalBatchCount;
        private int finishedBatchCount;
        private int totalSkuCount;
        private int formalSuccessCount;
        private int shadowSuccessCount;
        private int governanceCount;
        private int failureCount;
        private final List<String> errorMessages = new ArrayList<>();

        private void add(EleStoreInventoryBatchTaskStoreDO store) {
            String status = store.getStatus();
            if (STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status) || STATUS_PARTIAL_FAIL.equals(status)
                    || STATUS_CANCELLED.equals(status)) {
                finishedStoreCount++;
            }
            if (STATUS_FAILED.equals(status) || STATUS_PARTIAL_FAIL.equals(status)) {
                failedStoreCount++;
            }
            totalBatchCount += valueOf(store.getTotalBatchNo());
            finishedBatchCount += valueOf(store.getCurrentBatchNo());
            totalSkuCount += valueOf(store.getTotalSkuCount());
            formalSuccessCount += valueOf(store.getFormalSuccessCount());
            shadowSuccessCount += valueOf(store.getShadowSuccessCount());
            governanceCount += valueOf(store.getGovernanceCount());
            failureCount += valueOf(store.getFailureCount());
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
