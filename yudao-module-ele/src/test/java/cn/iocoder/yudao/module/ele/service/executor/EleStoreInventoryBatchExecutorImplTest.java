package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleSkuInventoryQueryService;
import cn.iocoder.yudao.module.ele.service.EleStoreInventorySkuScopeService;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;
import cn.iocoder.yudao.module.ele.service.guard.StoreExecutionGuard;
import cn.iocoder.yudao.module.ele.service.guard.StoreExecutionScenario;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreInventoryBatchExecutorImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryBatchExecutorImpl executor;

    @Mock
    private EleStoreInventoryBatchTaskMapper taskMapper;
    @Mock
    private EleStoreInventoryBatchTaskStoreMapper taskStoreMapper;
    @Mock
    private EleSkuInventoryQueryService queryService;
    @Mock
    private EleStoreInventorySkuScopeService skuScopeService;
    @Mock
    private ThreadPoolTaskExecutor batchExecutor;
    @Mock
    private StoreExecutionGuard storeExecutionGuard;

    @Test
    void execute_whenGuardRejects_thenMarkStoreFailed() {
        EleStoreInventoryBatchTaskDO task = buildTask();
        EleStoreInventoryBatchTaskStoreDO taskStore = buildTaskStore();
        prepareSingleStoreTask(task, taskStore);
        doThrow(new IllegalStateException("门店库存执行获取执行锁失败: platform-store-1"))
                .when(storeExecutionGuard).runWithStoreLock(eq(StoreExecutionScenario.STORE_INVENTORY),
                        eq("platform-store-1"), any(Runnable.class));

        executor.execute(1L);

        verify(queryService, never()).queryBatch(any());
        verify(taskStoreMapper).finishIfRunning(eq(11L), eq("FAILED"), argThat((EleStoreInventoryBatchTaskStoreDO updateObj) ->
                Integer.valueOf(1).equals(updateObj.getFailureCount())
                        && updateObj.getErrorMsg() != null
                        && updateObj.getErrorMsg().contains("获取执行锁失败")));
    }

    @Test
    void execute_whenMissingRowAlreadyCountedInResponse_thenDoNotDoubleCountFailure() {
        EleStoreInventoryBatchTaskDO task = buildTask();
        EleStoreInventoryBatchTaskStoreDO taskStore = buildTaskStore();
        prepareSingleStoreTask(task, taskStore);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(2);
            runnable.run();
            return null;
        }).when(storeExecutionGuard).runWithStoreLock(eq(StoreExecutionScenario.STORE_INVENTORY),
                eq("platform-store-1"), any(Runnable.class));
        when(skuScopeService.listStoreSkuScope("store-1", "erp-store-1")).thenReturn(List.of("SKU-1"));

        EleSkuInventoryBatchQueryRespDTO respDTO = new EleSkuInventoryBatchQueryRespDTO();
        respDTO.setFailureCount(1);
        respDTO.setMissingRowCount(1);
        respDTO.getErrorDetails().add("INVENTORY_ROW_MISSING:SKU:SKU-1");
        when(queryService.queryBatch(any())).thenReturn(respDTO);

        executor.execute(1L);

        verify(taskStoreMapper).finishIfRunning(eq(11L), eq("FAILED"), argThat((EleStoreInventoryBatchTaskStoreDO updateObj) ->
                Integer.valueOf(1).equals(updateObj.getFailureCount())));
    }

    @Test
    void execute_whenRecoveryAlreadyFailedTaskStore_thenConditionalWritesDoNotOverwrite() {
        EleStoreInventoryBatchTaskDO task = buildTask();
        task.setStatus("RUNNING");
        EleStoreInventoryBatchTaskStoreDO taskStore = buildTaskStore();
        prepareSingleStoreTask(task, taskStore);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(2);
            runnable.run();
            return null;
        }).when(storeExecutionGuard).runWithStoreLock(eq(StoreExecutionScenario.STORE_INVENTORY),
                eq("platform-store-1"), any(Runnable.class));
        when(skuScopeService.listStoreSkuScope("store-1", "erp-store-1")).thenReturn(List.of("SKU-1"));
        when(taskMapper.selectById(1L)).thenReturn(task, task, failedTaskSnapshot());
        when(taskStoreMapper.markRunningIfPending(eq(11L), any())).thenReturn(1);
        when(taskStoreMapper.updateProgressIfRunning(eq(11L), any(EleStoreInventoryBatchTaskStoreDO.class))).thenReturn(0);
        when(taskStoreMapper.finishIfRunning(eq(11L), any(String.class), any(EleStoreInventoryBatchTaskStoreDO.class))).thenReturn(0);
        when(taskMapper.refreshAggregateIfRunning(eq(1L), any(EleStoreInventoryBatchTaskDO.class))).thenReturn(0);
        when(taskMapper.finishIfRunning(eq(1L), any(String.class), any(EleStoreInventoryBatchTaskDO.class))).thenReturn(0);

        EleSkuInventoryBatchQueryRespDTO respDTO = new EleSkuInventoryBatchQueryRespDTO();
        respDTO.setFormalSuccessCount(1);
        when(queryService.queryBatch(any())).thenReturn(respDTO);

        executor.execute(1L);

        verify(taskStoreMapper).updateProgressIfRunning(eq(11L), argThat((EleStoreInventoryBatchTaskStoreDO updateObj) ->
                Integer.valueOf(1).equals(updateObj.getCurrentBatchNo())
                        && Integer.valueOf(1).equals(updateObj.getFormalSuccessCount())));
        verify(taskStoreMapper).finishIfRunning(eq(11L), eq("SUCCESS"), any(EleStoreInventoryBatchTaskStoreDO.class));
        verify(taskMapper).finishIfRunning(eq(1L), eq("SUCCESS"), any(EleStoreInventoryBatchTaskDO.class));
    }

    private void prepareSingleStoreTask(EleStoreInventoryBatchTaskDO task, EleStoreInventoryBatchTaskStoreDO taskStore) {
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.markRunningIfPending(eq(1L), any())).thenReturn(1);
        when(taskStoreMapper.selectListByTaskId(1L)).thenReturn(List.of(taskStore));
        when(batchExecutor.getMaxPoolSize()).thenReturn(1);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(batchExecutor).execute(any(Runnable.class));
        lenient().when(taskStoreMapper.markRunningIfPending(eq(11L), any())).thenReturn(1);
        lenient().when(taskStoreMapper.updateProgressIfRunning(eq(11L), any(EleStoreInventoryBatchTaskStoreDO.class))).thenAnswer(invocation -> {
            EleStoreInventoryBatchTaskStoreDO updateObj = invocation.getArgument(1);
            applyStoreUpdate(taskStore, updateObj);
            return 1;
        });
        lenient().when(taskStoreMapper.finishIfRunning(eq(11L), any(String.class), any(EleStoreInventoryBatchTaskStoreDO.class))).thenAnswer(invocation -> {
            String status = invocation.getArgument(1);
            EleStoreInventoryBatchTaskStoreDO updateObj = invocation.getArgument(2);
            taskStore.setStatus(status);
            applyStoreUpdate(taskStore, updateObj);
            return 1;
        });
        lenient().when(taskMapper.refreshAggregateIfRunning(eq(1L), any(EleStoreInventoryBatchTaskDO.class))).thenReturn(1);
        lenient().when(taskMapper.finishIfRunning(eq(1L), any(String.class), any(EleStoreInventoryBatchTaskDO.class))).thenReturn(1);
    }

    private void applyStoreUpdate(EleStoreInventoryBatchTaskStoreDO target, EleStoreInventoryBatchTaskStoreDO updateObj) {
        if (updateObj.getCurrentBatchNo() != null) {
            target.setCurrentBatchNo(updateObj.getCurrentBatchNo());
        }
        if (updateObj.getTotalBatchNo() != null) {
            target.setTotalBatchNo(updateObj.getTotalBatchNo());
        }
        if (updateObj.getTotalSkuCount() != null) {
            target.setTotalSkuCount(updateObj.getTotalSkuCount());
        }
        if (updateObj.getFormalSuccessCount() != null) {
            target.setFormalSuccessCount(updateObj.getFormalSuccessCount());
        }
        if (updateObj.getShadowSuccessCount() != null) {
            target.setShadowSuccessCount(updateObj.getShadowSuccessCount());
        }
        if (updateObj.getGovernanceCount() != null) {
            target.setGovernanceCount(updateObj.getGovernanceCount());
        }
        if (updateObj.getFailureCount() != null) {
            target.setFailureCount(updateObj.getFailureCount());
        }
        if (updateObj.getErrorMsg() != null) {
            target.setErrorMsg(updateObj.getErrorMsg());
        }
    }

    private EleStoreInventoryBatchTaskDO failedTaskSnapshot() {
        EleStoreInventoryBatchTaskDO task = new EleStoreInventoryBatchTaskDO();
        task.setId(1L);
        task.setStatus("FAILED");
        return task;
    }

    private EleStoreInventoryBatchTaskDO buildTask() {
        EleStoreInventoryBatchTaskDO task = new EleStoreInventoryBatchTaskDO();
        task.setId(1L);
        task.setStatus("PENDING");
        return task;
    }

    private EleStoreInventoryBatchTaskStoreDO buildTaskStore() {
        EleStoreInventoryBatchTaskStoreDO taskStore = new EleStoreInventoryBatchTaskStoreDO();
        taskStore.setId(11L);
        taskStore.setTaskId(1L);
        taskStore.setStoreId("store-1");
        taskStore.setStoreName("测试门店");
        taskStore.setPlatformStoreId("platform-store-1");
        taskStore.setMerchantCode("merchant-1");
        taskStore.setErpStoreCode("erp-store-1");
        taskStore.setStatus("PENDING");
        return taskStore;
    }
}
