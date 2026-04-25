package cn.iocoder.yudao.module.ele.service.executor;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncService;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void execute_shouldSyncStorePagesAndMarkTaskSuccess() {
        EleStoreGoodsFullSyncTaskDO task = new EleStoreGoodsFullSyncTaskDO();
        task.setId(1L);
        task.setTaskNo("TASK001");
        task.setTestMode(false);
        when(taskMapper.selectById(1L)).thenReturn(task);
        EleStoreGoodsFullSyncTaskStoreDO taskStore = new EleStoreGoodsFullSyncTaskStoreDO();
        taskStore.setId(10L);
        taskStore.setTaskId(1L);
        taskStore.setMerchantCode("MERCHANT001");
        taskStore.setErpStoreCode("STORE001");
        taskStore.setPageSize(20);
        when(taskStoreMapper.selectListByTaskId(1L)).thenReturn(List.of(taskStore));
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
