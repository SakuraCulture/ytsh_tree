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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class EleStoreGoodsFullSyncExecutorImpl implements EleStoreGoodsFullSyncExecutor {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PARTIAL_FAIL = "PARTIAL_FAIL";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Resource
    private EleStoreGoodsFullSyncTaskMapper taskMapper;
    @Resource
    private EleStoreGoodsFullSyncTaskStoreMapper taskStoreMapper;
    @Resource
    private EleStoreGoodsSyncService syncService;

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
        TaskAggregate aggregate = new TaskAggregate();
        for (EleStoreGoodsFullSyncTaskStoreDO taskStore : taskStores) {
            if (isCancelled(taskId)) {
                return;
            }
            StoreSyncSummary storeSummary = syncStore(task, taskStore);
            aggregate.add(storeSummary);
        }
        if (!isCancelled(taskId)) {
            finishTask(taskId, aggregate);
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
                summary.totalSkuCount = value(pageResult.getTotal());
                summary.successCount += value(pageResult.getSuccessCount());
                summary.failCount += value(pageResult.getFailCount());
                summary.governanceCount += value(pageResult.getGovernanceCount());
                updateStoreProgress(taskStore.getId(), pageNo, totalPage, pageSize, summary);
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

    private void finishTask(Long taskId, TaskAggregate aggregate) {
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

        private void add(StoreSyncSummary summary) {
            finishedStoreCount++;
            if (!summary.success) {
                failedStoreCount++;
            }
            totalPageCount += summary.totalPage;
            finishedPageCount += summary.finishedPage;
            totalSkuCount += summary.totalSkuCount;
            successCount += summary.successCount;
            failCount += summary.failCount;
            governanceCount += summary.governanceCount;
        }
    }
}
