package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchCurrentReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryBatchTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryBatchTaskStoreMapper;
import cn.iocoder.yudao.module.ele.dal.redis.EleOrderLockService;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreInventoryBatchExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EleStoreInventoryBatchServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryBatchServiceImpl batchService;

    @Mock
    private EleStoreInventoryBatchTaskMapper taskMapper;
    @Mock
    private EleStoreInventoryBatchTaskStoreMapper taskStoreMapper;
    @Mock
    private StoreService storeService;
    @Mock
    private EleStoreInventoryBatchExecutor batchExecutor;
    @Mock
    private EleOrderLockService eleOrderLockService;

    @Test
    public void testCreateCurrentStoreBatchTask_whenNoRunningTask_thenPersistStoreIdentityAndSubmit() {
        EleStoreInventoryBatchCurrentReqVO reqVO = new EleStoreInventoryBatchCurrentReqVO();
        reqVO.setMerchantCode("merchant-1");
        reqVO.setErpStoreCode("erp-store-1");

        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("store-1");
        store.setPlatformStoreName("测试门店");
        when(storeService.getPlatformTableByPlatformStoreId("erp-store-1")).thenReturn(store);
        when(taskMapper.selectRunningCurrentStore("erp-store-1")).thenReturn(null);
        doAnswer(invocation -> {
            EleStoreInventoryBatchTaskDO task = invocation.getArgument(0);
            task.setId(123L);
            return 1;
        }).when(taskMapper).insert(any(EleStoreInventoryBatchTaskDO.class));

        Long taskId = batchService.createCurrentStoreBatchTask(reqVO);

        assertEquals(123L, taskId);
        verify(eleOrderLockService).lockStoreInventoryBatchTask("CURRENT_STORE:erp-store-1", 5, 1);
        verify(taskMapper).insert(argThat((EleStoreInventoryBatchTaskDO task) -> "MANUAL".equals(task.getSourceType())
                && "CURRENT_STORE".equals(task.getScope())
                && "PENDING".equals(task.getStatus())
                && Integer.valueOf(1).equals(task.getTotalStoreCount())
                && task.getTaskNo() != null
                && !task.getTaskNo().isBlank()));
        verify(taskStoreMapper).insert(argThat((EleStoreInventoryBatchTaskStoreDO taskStore) -> Long.valueOf(123L).equals(taskStore.getTaskId())
                && "merchant-1".equals(taskStore.getMerchantCode())
                && "erp-store-1".equals(taskStore.getErpStoreCode())
                && "erp-store-1".equals(taskStore.getPlatformStoreId())
                && "store-1".equals(taskStore.getStoreId())
                && "测试门店".equals(taskStore.getStoreName())
                && "PENDING".equals(taskStore.getStatus())));
        verify(batchExecutor).submit(123L);
        verify(eleOrderLockService).unlockStoreInventoryBatchTask("CURRENT_STORE:erp-store-1");
    }

    @Test
    public void testCreateCurrentStoreBatchTask_whenRunningTaskExists_thenReturnExistingTaskId() {
        EleStoreInventoryBatchCurrentReqVO reqVO = new EleStoreInventoryBatchCurrentReqVO();
        reqVO.setMerchantCode("merchant-1");
        reqVO.setErpStoreCode("erp-store-1");
        reqVO.setPlatformStoreId("platform-store-1");

        EleStoreInventoryBatchTaskDO runningTask = new EleStoreInventoryBatchTaskDO();
        runningTask.setId(456L);
        when(storeService.getPlatformTableByPlatformStoreId("platform-store-1")).thenReturn(new StorePlatformRespVO());
        when(taskMapper.selectRunningCurrentStore("erp-store-1")).thenReturn(runningTask);

        Long taskId = batchService.createCurrentStoreBatchTask(reqVO);

        assertEquals(456L, taskId);
        verify(eleOrderLockService).lockStoreInventoryBatchTask("CURRENT_STORE:erp-store-1", 5, 1);
        verify(taskMapper, never()).insert(any(EleStoreInventoryBatchTaskDO.class));
        verify(taskStoreMapper, never()).insert(any(EleStoreInventoryBatchTaskStoreDO.class));
        verify(batchExecutor, never()).submit(any(Long.class));
        verify(eleOrderLockService).unlockStoreInventoryBatchTask("CURRENT_STORE:erp-store-1");
    }
}
