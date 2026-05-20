package cn.iocoder.yudao.module.ele.service.recovery;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreGoodsFullSyncExecutor;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreInventoryBatchExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SyncTaskRecoveryService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_FAILED = "FAILED";
    private static final String RECOVERY_TIMEOUT = "RECOVERY_TIMEOUT";

    @Resource
    private EleStoreGoodsFullSyncTaskMapper goodsTaskMapper;
    @Resource
    private EleStoreGoodsFullSyncTaskStoreMapper goodsTaskStoreMapper;
    @Resource
    private EleStoreInventoryBatchTaskMapper inventoryTaskMapper;
    @Resource
    private EleStoreInventoryBatchTaskStoreMapper inventoryTaskStoreMapper;
    @Resource
    private EleStoreGoodsFullSyncExecutor goodsFullSyncExecutor;
    @Resource
    private EleStoreInventoryBatchExecutor inventoryBatchExecutor;

    @Value("${ele.sync.recovery.pending-timeout-minutes:10}")
    private long pendingTimeoutMinutes;
    @Value("${ele.sync.recovery.running-timeout-minutes:120}")
    private long runningTimeoutMinutes;

    public String recoverTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pendingTimeoutAt = now.minusMinutes(pendingTimeoutMinutes);
        LocalDateTime runningTimeoutAt = now.minusMinutes(runningTimeoutMinutes);
        log.info("【同步任务恢复】开始扫描超时任务, pendingTimeoutAt={}, runningTimeoutAt={}", pendingTimeoutAt, runningTimeoutAt);

        int goodsPendingResubmitted = recoverGoodsPendingTasks(pendingTimeoutAt);
        int goodsRunningFailed = recoverGoodsRunningTasks(runningTimeoutAt, now);
        int inventoryPendingResubmitted = recoverInventoryPendingTasks(pendingTimeoutAt);
        int inventoryRunningFailed = recoverInventoryRunningTasks(runningTimeoutAt, now);

        String result = String.format(
                "同步任务恢复完成 goodsPendingResubmitted=%d goodsRunningFailed=%d inventoryPendingResubmitted=%d inventoryRunningFailed=%d",
                goodsPendingResubmitted, goodsRunningFailed, inventoryPendingResubmitted, inventoryRunningFailed);
        log.info("【同步任务恢复】{}", result);
        return result;
    }

    private int recoverGoodsPendingTasks(LocalDateTime pendingTimeoutAt) {
        List<EleStoreGoodsFullSyncTaskDO> tasks = goodsTaskMapper.selectTimeoutPendingTasks(pendingTimeoutAt);
        int recovered = 0;
        for (EleStoreGoodsFullSyncTaskDO task : tasks) {
            EleStoreGoodsFullSyncTaskDO currentTask = goodsTaskMapper.selectById(task.getId());
            if (currentTask == null || !STATUS_PENDING.equals(currentTask.getStatus())) {
                log.info("【同步任务恢复】跳过商品待恢复任务, taskId={}, currentStatus={}", task.getId(),
                        currentTask == null ? null : currentTask.getStatus());
                continue;
            }
            try {
                log.warn("【同步任务恢复】发现商品任务提交丢失, taskId={}, taskNo={}, status={}",
                        currentTask.getId(), currentTask.getTaskNo(), currentTask.getStatus());
                goodsFullSyncExecutor.submit(currentTask.getId());
                recovered++;
            } catch (Exception ex) {
                log.error("【同步任务恢复】商品任务重新提交失败, taskId={}", currentTask.getId(), ex);
            }
        }
        return recovered;
    }

    private int recoverGoodsRunningTasks(LocalDateTime runningTimeoutAt, LocalDateTime recoveredAt) {
        List<EleStoreGoodsFullSyncTaskDO> tasks = goodsTaskMapper.selectTimeoutRunningTasks(runningTimeoutAt);
        int recovered = 0;
        for (EleStoreGoodsFullSyncTaskDO task : tasks) {
            EleStoreGoodsFullSyncTaskDO currentTask = goodsTaskMapper.selectById(task.getId());
            if (currentTask == null || !STATUS_RUNNING.equals(currentTask.getStatus())) {
                log.info("【同步任务恢复】跳过商品运行中恢复任务, taskId={}, currentStatus={}", task.getId(),
                        currentTask == null ? null : currentTask.getStatus());
                continue;
            }
            String errorMsg = buildRecoveryTimeoutMessage(currentTask.getTaskNo(), recoveredAt, runningTimeoutMinutes);
            int updated = goodsTaskMapper.markFailedIfRunning(currentTask.getId(), errorMsg, recoveredAt);
            if (updated == 0) {
                log.info("【同步任务恢复】跳过商品运行中恢复写回, taskId={}, currentStatus={}", currentTask.getId(), currentTask.getStatus());
                continue;
            }
            int storeRows = goodsTaskStoreMapper.failUnfinishedByTaskId(currentTask.getId(), errorMsg, recoveredAt);
            recovered++;
            log.warn("【同步任务恢复】商品运行中任务超时转失败, taskId={}, taskNo={}, affectedStores={}",
                    currentTask.getId(), currentTask.getTaskNo(), storeRows);
        }
        return recovered;
    }

    private int recoverInventoryPendingTasks(LocalDateTime pendingTimeoutAt) {
        List<EleStoreInventoryBatchTaskDO> tasks = inventoryTaskMapper.selectTimeoutPendingTasks(pendingTimeoutAt);
        int recovered = 0;
        for (EleStoreInventoryBatchTaskDO task : tasks) {
            EleStoreInventoryBatchTaskDO currentTask = inventoryTaskMapper.selectById(task.getId());
            if (currentTask == null || !STATUS_PENDING.equals(currentTask.getStatus())) {
                log.info("【同步任务恢复】跳过库存待恢复任务, taskId={}, currentStatus={}", task.getId(),
                        currentTask == null ? null : currentTask.getStatus());
                continue;
            }
            try {
                log.warn("【同步任务恢复】发现库存任务提交丢失, taskId={}, taskNo={}, status={}",
                        currentTask.getId(), currentTask.getTaskNo(), currentTask.getStatus());
                inventoryBatchExecutor.submit(currentTask.getId());
                recovered++;
            } catch (Exception ex) {
                log.error("【同步任务恢复】库存任务重新提交失败, taskId={}", currentTask.getId(), ex);
            }
        }
        return recovered;
    }

    private int recoverInventoryRunningTasks(LocalDateTime runningTimeoutAt, LocalDateTime recoveredAt) {
        List<EleStoreInventoryBatchTaskDO> tasks = inventoryTaskMapper.selectTimeoutRunningTasks(runningTimeoutAt);
        int recovered = 0;
        for (EleStoreInventoryBatchTaskDO task : tasks) {
            EleStoreInventoryBatchTaskDO currentTask = inventoryTaskMapper.selectById(task.getId());
            if (currentTask == null || !STATUS_RUNNING.equals(currentTask.getStatus())) {
                log.info("【同步任务恢复】跳过库存运行中恢复任务, taskId={}, currentStatus={}", task.getId(),
                        currentTask == null ? null : currentTask.getStatus());
                continue;
            }
            String errorMsg = buildRecoveryTimeoutMessage(currentTask.getTaskNo(), recoveredAt, runningTimeoutMinutes);
            int updated = inventoryTaskMapper.markFailedIfRunning(currentTask.getId(), errorMsg, recoveredAt);
            if (updated == 0) {
                log.info("【同步任务恢复】跳过库存运行中恢复写回, taskId={}, currentStatus={}", currentTask.getId(), currentTask.getStatus());
                continue;
            }
            int storeRows = inventoryTaskStoreMapper.failUnfinishedByTaskId(currentTask.getId(), errorMsg, recoveredAt);
            recovered++;
            log.warn("【同步任务恢复】库存运行中任务超时转失败, taskId={}, taskNo={}, affectedStores={}",
                    currentTask.getId(), currentTask.getTaskNo(), storeRows);
        }
        return recovered;
    }

    private String buildRecoveryTimeoutMessage(String taskNo, LocalDateTime recoveredAt, long timeoutMinutes) {
        return String.format("%s: taskNo=%s exceeded %d minutes, recoveredAt=%s",
                RECOVERY_TIMEOUT, taskNo, timeoutMinutes, recoveredAt);
    }
}
