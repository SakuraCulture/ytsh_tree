package cn.iocoder.yudao.module.ele.service.recovery;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreGoodsFullSyncExecutor;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreInventoryBatchExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncTaskRecoveryServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private SyncTaskRecoveryService syncTaskRecoveryService;

    @Mock
    private EleStoreGoodsFullSyncTaskMapper goodsTaskMapper;
    @Mock
    private EleStoreGoodsFullSyncTaskStoreMapper goodsTaskStoreMapper;
    @Mock
    private EleStoreInventoryBatchTaskMapper inventoryTaskMapper;
    @Mock
    private EleStoreInventoryBatchTaskStoreMapper inventoryTaskStoreMapper;
    @Mock
    private EleStoreGoodsFullSyncExecutor goodsFullSyncExecutor;
    @Mock
    private EleStoreInventoryBatchExecutor inventoryBatchExecutor;

    @Test
    public void testRecoverTasks_whenGoodsPendingTimeout_thenResubmitTask() {
        ReflectionTestUtils.setField(syncTaskRecoveryService, "pendingTimeoutMinutes", 30L);
        ReflectionTestUtils.setField(syncTaskRecoveryService, "runningTimeoutMinutes", 120L);

        EleStoreGoodsFullSyncTaskDO task = new EleStoreGoodsFullSyncTaskDO();
        task.setId(11L);
        task.setTaskNo("goods-pending-11");
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now().minusMinutes(40));
        when(goodsTaskMapper.selectTimeoutPendingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        when(goodsTaskMapper.selectTimeoutRunningTasks(any(LocalDateTime.class))).thenReturn(List.of());
        when(goodsTaskMapper.selectById(11L)).thenReturn(task);
        when(inventoryTaskMapper.selectTimeoutPendingTasks(any(LocalDateTime.class))).thenReturn(List.of());
        when(inventoryTaskMapper.selectTimeoutRunningTasks(any(LocalDateTime.class))).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now();
        String result = syncTaskRecoveryService.recoverTasks();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> pendingCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(goodsTaskMapper).selectTimeoutPendingTasks(pendingCutoffCaptor.capture());
        LocalDateTime pendingCutoff = pendingCutoffCaptor.getValue();
        assertFalse(pendingCutoff.isBefore(before.minusMinutes(30)));
        assertFalse(pendingCutoff.isAfter(after.minusMinutes(30)));
        verify(goodsFullSyncExecutor).submit(11L);
        verify(goodsTaskMapper, never()).markFailedIfRunning(any(Long.class), any(String.class), any(LocalDateTime.class));
        assertTrue(result.contains("goodsPendingResubmitted=1"));
    }

    @Test
    public void testRecoverTasks_whenInventoryRunningTimeout_thenFailTaskAndStores() {
        ReflectionTestUtils.setField(syncTaskRecoveryService, "pendingTimeoutMinutes", 10L);
        ReflectionTestUtils.setField(syncTaskRecoveryService, "runningTimeoutMinutes", 90L);

        EleStoreInventoryBatchTaskDO task = new EleStoreInventoryBatchTaskDO();
        task.setId(22L);
        task.setTaskNo("inventory-running-22");
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now().minusHours(2));
        when(goodsTaskMapper.selectTimeoutPendingTasks(any(LocalDateTime.class))).thenReturn(List.of());
        when(goodsTaskMapper.selectTimeoutRunningTasks(any(LocalDateTime.class))).thenReturn(List.of());
        when(inventoryTaskMapper.selectTimeoutPendingTasks(any(LocalDateTime.class))).thenReturn(List.of());
        when(inventoryTaskMapper.selectTimeoutRunningTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        when(inventoryTaskMapper.selectById(22L)).thenReturn(task);
        when(inventoryTaskMapper.markFailedIfRunning(eq(22L), contains("RECOVERY_TIMEOUT"), any(LocalDateTime.class))).thenReturn(1);

        LocalDateTime before = LocalDateTime.now();
        String result = syncTaskRecoveryService.recoverTasks();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> runningCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(inventoryTaskMapper).selectTimeoutRunningTasks(runningCutoffCaptor.capture());
        LocalDateTime runningCutoff = runningCutoffCaptor.getValue();
        assertFalse(runningCutoff.isBefore(before.minusMinutes(90)));
        assertFalse(runningCutoff.isAfter(after.minusMinutes(90)));
        verify(inventoryTaskMapper).markFailedIfRunning(eq(22L), contains("RECOVERY_TIMEOUT"), any(LocalDateTime.class));
        verify(inventoryTaskStoreMapper).failUnfinishedByTaskId(eq(22L), contains("RECOVERY_TIMEOUT"), any(LocalDateTime.class));
        verify(inventoryBatchExecutor, never()).submit(any(Long.class));
        assertTrue(result.contains("inventoryRunningFailed=1"));
    }
}
