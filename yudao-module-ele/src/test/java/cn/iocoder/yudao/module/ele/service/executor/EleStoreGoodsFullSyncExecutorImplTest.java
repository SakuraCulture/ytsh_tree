package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncService;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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

    private ThreadPoolTaskExecutor fullSyncExecutor;

    @BeforeEach
    void setUpExecutor() {
        fullSyncExecutor = new ThreadPoolTaskExecutor();
        fullSyncExecutor.setCorePoolSize(1);
        fullSyncExecutor.setMaxPoolSize(1);
        fullSyncExecutor.setQueueCapacity(1);
        fullSyncExecutor.initialize();
        ReflectionTestUtils.setField(executor, "fullSyncExecutor", fullSyncExecutor);
    }

    @Test
    void execute_shouldSyncStorePagesAndMarkTaskSuccess() {
        EleStoreGoodsFullSyncTaskDO task = task(1L, false);
        when(taskMapper.selectById(1L)).thenAnswer(invocation -> task(1L, false));
        EleStoreGoodsFullSyncTaskStoreDO taskStore = taskStore(10L, 1L, "MERCHANT001", "STORE001");
        List<EleStoreGoodsFullSyncTaskStoreDO> taskStores = mutableTaskStores(taskStore);
        when(taskStoreMapper.selectListByTaskId(1L)).thenReturn(taskStores);
        mockTaskStoreUpdates(taskStores);
        when(syncService.syncStoreGoodsPage(any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(pageResult(1, 20, 25, 20, 20, 0, 0))
                .thenReturn(pageResult(2, 20, 25, 5, 4, 1, 1));

        executor.execute(1L);

        ArgumentCaptor<EleStoreGoodsFullSyncTaskStoreDO> storeCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskStoreDO.class);
        verify(taskStoreMapper, org.mockito.Mockito.atLeastOnce()).updateById(storeCaptor.capture());
        EleStoreGoodsFullSyncTaskStoreDO finishedStore = storeCaptor.getAllValues().get(storeCaptor.getAllValues().size() - 1);
        assertEquals(10L, finishedStore.getId());
        assertEquals("SUCCESS", finishedStore.getStatus());
        assertEquals(2, finishedStore.getCurrentPage());
        assertEquals(2, finishedStore.getTotalPage());
        assertEquals(25, finishedStore.getTotalSkuCount());
        assertEquals(24, finishedStore.getSuccessCount());
        assertEquals(1, finishedStore.getFailCount());
        assertEquals(1, finishedStore.getGovernanceCount());

        ArgumentCaptor<EleStoreGoodsFullSyncTaskDO> taskCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateById(taskCaptor.capture());
        EleStoreGoodsFullSyncTaskDO finishedTask = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(1L, finishedTask.getId());
        assertEquals("SUCCESS", finishedTask.getStatus());
        assertEquals(1, finishedTask.getFinishedStoreCount());
        assertEquals(2, finishedTask.getTotalPageCount());
        assertEquals(2, finishedTask.getFinishedPageCount());
        assertEquals(25, finishedTask.getTotalSkuCount());
        assertEquals(24, finishedTask.getSuccessCount());
        assertEquals(1, finishedTask.getFailCount());
        assertEquals(1, finishedTask.getGovernanceCount());
    }

    @Test
    void execute_shouldProcessMultipleStoresWithinBoundedDispatchWindow() {
        EleStoreGoodsFullSyncTaskDO task = task(2L, false);
        when(taskMapper.selectById(2L)).thenAnswer(invocation -> task(2L, false));
        EleStoreGoodsFullSyncTaskStoreDO firstStore = taskStore(21L, 2L, "MERCHANT001", "STORE001");
        EleStoreGoodsFullSyncTaskStoreDO secondStore = taskStore(22L, 2L, "MERCHANT002", "STORE002");
        EleStoreGoodsFullSyncTaskStoreDO thirdStore = taskStore(23L, 2L, "MERCHANT003", "STORE003");
        List<EleStoreGoodsFullSyncTaskStoreDO> taskStores = mutableTaskStores(firstStore, secondStore, thirdStore);
        when(taskStoreMapper.selectListByTaskId(2L)).thenReturn(taskStores);
        mockTaskStoreUpdates(taskStores);
        when(syncService.syncStoreGoodsPage(any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(pageResult(1, 20, 1, 1, 1, 0, 0))
                .thenReturn(pageResult(1, 20, 1, 1, 1, 0, 0))
                .thenReturn(pageResult(1, 20, 1, 1, 1, 0, 0));

        executor.execute(2L);

        verify(syncService, org.mockito.Mockito.times(3)).syncStoreGoodsPage(any(), org.mockito.ArgumentMatchers.eq(false));
        ArgumentCaptor<EleStoreGoodsFullSyncTaskDO> taskCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateById(taskCaptor.capture());
        EleStoreGoodsFullSyncTaskDO finishedTask = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals("SUCCESS", finishedTask.getStatus());
        assertEquals(3, finishedTask.getFinishedStoreCount());
        assertEquals(3, finishedTask.getTotalPageCount());
        assertEquals(3, finishedTask.getFinishedPageCount());
        assertEquals(3, finishedTask.getTotalSkuCount());
        assertEquals(3, finishedTask.getSuccessCount());
        assertEquals(0, finishedTask.getFailCount());
        assertEquals(0, finishedTask.getGovernanceCount());
    }

    @Test
    void execute_shouldNotFinishTaskWhenCancelledDuringDispatch() {
        EleStoreGoodsFullSyncTaskDO runningTask = task(3L, false);
        EleStoreGoodsFullSyncTaskDO cancelledTask = task(3L, false);
        cancelledTask.setStatus("CANCELLED");
        when(taskMapper.selectById(3L)).thenReturn(runningTask, runningTask, cancelledTask, cancelledTask, cancelledTask);
        EleStoreGoodsFullSyncTaskStoreDO firstStore = taskStore(31L, 3L, "MERCHANT001", "STORE001");
        EleStoreGoodsFullSyncTaskStoreDO secondStore = taskStore(32L, 3L, "MERCHANT002", "STORE002");
        List<EleStoreGoodsFullSyncTaskStoreDO> taskStores = mutableTaskStores(firstStore, secondStore);
        when(taskStoreMapper.selectListByTaskId(3L)).thenReturn(taskStores);
        mockTaskStoreUpdates(taskStores);

        executor.execute(3L);

        verify(syncService, never()).syncStoreGoodsPage(any(), org.mockito.ArgumentMatchers.eq(false));
        verify(taskMapper, never()).updateById(argThat((EleStoreGoodsFullSyncTaskDO update) -> "SUCCESS".equals(update.getStatus()) || "PARTIAL_FAIL".equals(update.getStatus())));
        assertEquals("CANCELLED", firstStore.getStatus());
        assertEquals(null, secondStore.getStatus());
    }

    private EleStoreGoodsFullSyncTaskDO task(Long id, boolean testMode) {
        EleStoreGoodsFullSyncTaskDO task = new EleStoreGoodsFullSyncTaskDO();
        task.setId(id);
        task.setTaskNo("TASK" + id);
        task.setTestMode(testMode);
        return task;
    }

    private EleStoreGoodsFullSyncTaskStoreDO taskStore(Long id, Long taskId, String merchantCode, String erpStoreCode) {
        EleStoreGoodsFullSyncTaskStoreDO taskStore = new EleStoreGoodsFullSyncTaskStoreDO();
        taskStore.setId(id);
        taskStore.setTaskId(taskId);
        taskStore.setMerchantCode(merchantCode);
        taskStore.setErpStoreCode(erpStoreCode);
        taskStore.setStoreId(erpStoreCode);
        taskStore.setStoreName(erpStoreCode);
        taskStore.setPageSize(20);
        return taskStore;
    }

    private List<EleStoreGoodsFullSyncTaskStoreDO> mutableTaskStores(EleStoreGoodsFullSyncTaskStoreDO... stores) {
        return new ArrayList<>(List.of(stores));
    }

    private void mockTaskStoreUpdates(List<EleStoreGoodsFullSyncTaskStoreDO> taskStores) {
        doAnswer(invocation -> {
            EleStoreGoodsFullSyncTaskStoreDO updateObj = invocation.getArgument(0);
            for (EleStoreGoodsFullSyncTaskStoreDO taskStore : taskStores) {
                if (taskStore.getId().equals(updateObj.getId())) {
                    merge(taskStore, updateObj);
                    break;
                }
            }
            return 1;
        }).when(taskStoreMapper).updateById(any(EleStoreGoodsFullSyncTaskStoreDO.class));
    }

    private void merge(EleStoreGoodsFullSyncTaskStoreDO target, EleStoreGoodsFullSyncTaskStoreDO source) {
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
        if (source.getCurrentPage() != null) {
            target.setCurrentPage(source.getCurrentPage());
        }
        if (source.getTotalPage() != null) {
            target.setTotalPage(source.getTotalPage());
        }
        if (source.getPageSize() != null) {
            target.setPageSize(source.getPageSize());
        }
        if (source.getTotalSkuCount() != null) {
            target.setTotalSkuCount(source.getTotalSkuCount());
        }
        if (source.getSuccessCount() != null) {
            target.setSuccessCount(source.getSuccessCount());
        }
        if (source.getFailCount() != null) {
            target.setFailCount(source.getFailCount());
        }
        if (source.getGovernanceCount() != null) {
            target.setGovernanceCount(source.getGovernanceCount());
        }
        if (source.getErrorMsg() != null) {
            target.setErrorMsg(source.getErrorMsg());
        }
        if (source.getStartedAt() != null) {
            target.setStartedAt(source.getStartedAt());
        }
        if (source.getFinishedAt() != null) {
            target.setFinishedAt(source.getFinishedAt());
        }
    }

    private EleStoreGoodsPageSyncResult pageResult(int pageNo, int pageSize, int total, int syncCount,
                                                   int successCount, int failCount, int governanceCount) {
        EleStoreGoodsPageSyncResult result = new EleStoreGoodsPageSyncResult();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setSyncCount(syncCount);
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setGovernanceCount(governanceCount);
        return result;
    }
}
