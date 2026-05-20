package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncService;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreGoodsFullSyncExecutorImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreGoodsFullSyncExecutorImpl executor;

    @Mock
    private EleStoreGoodsFullSyncTaskMapper taskMapper;
    @Mock
    private EleStoreGoodsFullSyncTaskStoreMapper taskStoreMapper;
    @Mock
    private EleStoreGoodsSyncService syncService;
    @Mock
    private ThreadPoolTaskExecutor fullSyncExecutor;
    @Mock
    private StoreExecutionGuard storeExecutionGuard;

    @Test
    void execute_whenGuardRejects_thenMarkStoreFailedWithLockMessage() {
        EleStoreGoodsFullSyncTaskDO task = new EleStoreGoodsFullSyncTaskDO();
        task.setId(1L);
        task.setStatus("PENDING");

        EleStoreGoodsFullSyncTaskStoreDO taskStore = new EleStoreGoodsFullSyncTaskStoreDO();
        taskStore.setId(11L);
        taskStore.setTaskId(1L);
        taskStore.setPlatformStoreId("platform-store-1");
        taskStore.setMerchantCode("merchant-1");
        taskStore.setErpStoreCode("erp-store-1");
        taskStore.setStoreName("测试门店");
        taskStore.setPageSize(20);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.markRunningIfPending(eq(1L), any())).thenReturn(1);
        when(taskStoreMapper.selectListByTaskId(1L)).thenReturn(List.of(taskStore));
        when(fullSyncExecutor.getMaxPoolSize()).thenReturn(1);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(fullSyncExecutor).execute(any(Runnable.class));
        when(taskStoreMapper.markRunningIfPending(eq(11L), any())).thenReturn(1);
        doThrow(new IllegalStateException("门店商品执行获取执行锁失败: platform-store-1"))
                .when(storeExecutionGuard).runWithStoreLock(eq(StoreExecutionScenario.STORE_GOODS),
                        eq("platform-store-1"), any(Runnable.class));

        executor.execute(1L);

        verify(syncService, never()).syncStoreGoodsPage(any(), any());
        verify(taskStoreMapper).finishIfRunning(eq(11L), eq("FAILED"), argThat((EleStoreGoodsFullSyncTaskStoreDO updateObj) ->
                updateObj.getErrorMsg() != null && updateObj.getErrorMsg().contains("获取执行锁失败")));
    }

    @Test
    void execute_whenDuplicateSubmit_thenOnlyOneFlowClaimsAndRuns() {
        EleStoreGoodsFullSyncTaskDO task = new EleStoreGoodsFullSyncTaskDO();
        task.setId(1L);
        task.setStatus("PENDING");
        task.setTestMode(Boolean.FALSE);

        EleStoreGoodsFullSyncTaskStoreDO taskStore = new EleStoreGoodsFullSyncTaskStoreDO();
        taskStore.setId(11L);
        taskStore.setTaskId(1L);
        taskStore.setStoreId("store-1");
        taskStore.setStoreName("测试门店");
        taskStore.setPlatformStoreId("platform-store-1");
        taskStore.setMerchantCode("merchant-1");
        taskStore.setErpStoreCode("erp-store-1");
        taskStore.setStatus("PENDING");
        taskStore.setPageSize(20);

        EleStoreGoodsPageSyncResult pageResult = new EleStoreGoodsPageSyncResult();
        pageResult.setPageNo(1);
        pageResult.setPageSize(20);
        pageResult.setTotal(1);
        pageResult.setSyncCount(1);
        pageResult.setSuccessCount(1);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.markRunningIfPending(eq(1L), any())).thenReturn(1, 0);
        when(taskStoreMapper.selectListByTaskId(1L)).thenReturn(List.of(taskStore));
        when(fullSyncExecutor.getMaxPoolSize()).thenReturn(1);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(fullSyncExecutor).execute(any(Runnable.class));
        when(taskStoreMapper.markRunningIfPending(eq(11L), any())).thenReturn(1);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(2);
            runnable.run();
            return null;
        }).when(storeExecutionGuard).runWithStoreLock(eq(StoreExecutionScenario.STORE_GOODS),
                eq("platform-store-1"), any(Runnable.class));
        when(syncService.syncStoreGoodsPage(any(), eq(Boolean.FALSE))).thenReturn(pageResult);

        executor.execute(1L);
        executor.execute(1L);

        verify(taskMapper, times(2)).markRunningIfPending(eq(1L), any());
        verify(fullSyncExecutor, times(1)).execute(any(Runnable.class));
        verify(syncService, times(1)).syncStoreGoodsPage(any(), eq(Boolean.FALSE));
    }
}
