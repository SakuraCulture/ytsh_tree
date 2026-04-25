package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncCurrentReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsFullSyncTaskStoreDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsFullSyncTaskStoreMapper;
import cn.iocoder.yudao.module.ele.service.executor.EleStoreGoodsFullSyncExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreGoodsFullSyncServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreGoodsFullSyncServiceImpl fullSyncService;

    @Mock
    private EleStoreGoodsFullSyncTaskMapper taskMapper;
    @Mock
    private EleStoreGoodsFullSyncTaskStoreMapper taskStoreMapper;
    @Mock
    private StoreService storeService;
    @Mock
    private EleStoreGoodsFullSyncExecutor fullSyncExecutor;

    @Test
    void createCurrentStoreFullSync_shouldCreateTaskAndStoreThenSubmit() {
        EleStoreGoodsFullSyncCurrentReqVO reqVO = new EleStoreGoodsFullSyncCurrentReqVO();
        reqVO.setMerchantCode("MERCHANT001");
        reqVO.setErpStoreCode("STORE001");
        reqVO.setTestMode(true);
        when(taskMapper.selectRunningCurrentStore("STORE001")).thenReturn(null);
        doAnswer(invocation -> {
            EleStoreGoodsFullSyncTaskDO task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        }).when(taskMapper).insert(any(EleStoreGoodsFullSyncTaskDO.class));

        Long taskId = fullSyncService.createCurrentStoreFullSync(reqVO);

        assertEquals(10L, taskId);
        ArgumentCaptor<EleStoreGoodsFullSyncTaskDO> taskCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        EleStoreGoodsFullSyncTaskDO task = taskCaptor.getValue();
        assertNotNull(task.getTaskNo());
        assertEquals("CURRENT_STORE", task.getScope());
        assertEquals("MERCHANT001", task.getMerchantCode());
        assertEquals("STORE001", task.getErpStoreCode());
        assertEquals(true, task.getTestMode());
        assertEquals("PENDING", task.getStatus());
        assertEquals(1, task.getTotalStoreCount());

        ArgumentCaptor<EleStoreGoodsFullSyncTaskStoreDO> storeCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskStoreDO.class);
        verify(taskStoreMapper).insert(storeCaptor.capture());
        EleStoreGoodsFullSyncTaskStoreDO taskStore = storeCaptor.getValue();
        assertEquals(10L, taskStore.getTaskId());
        assertEquals(task.getTaskNo(), taskStore.getTaskNo());
        assertEquals("MERCHANT001", taskStore.getMerchantCode());
        assertEquals("STORE001", taskStore.getErpStoreCode());
        assertEquals("STORE001", taskStore.getPlatformStoreId());
        assertEquals("PENDING", taskStore.getStatus());
        verify(fullSyncExecutor).submit(10L);
    }

    @Test
    void createCurrentStoreFullSync_shouldReturnRunningTaskWhenExists() {
        EleStoreGoodsFullSyncCurrentReqVO reqVO = new EleStoreGoodsFullSyncCurrentReqVO();
        reqVO.setMerchantCode("MERCHANT001");
        reqVO.setErpStoreCode("STORE001");
        EleStoreGoodsFullSyncTaskDO runningTask = new EleStoreGoodsFullSyncTaskDO();
        runningTask.setId(99L);
        when(taskMapper.selectRunningCurrentStore("STORE001")).thenReturn(runningTask);

        Long taskId = fullSyncService.createCurrentStoreFullSync(reqVO);

        assertEquals(99L, taskId);
        verify(taskMapper, never()).insert(any(EleStoreGoodsFullSyncTaskDO.class));
        verify(taskStoreMapper, never()).insert(any(EleStoreGoodsFullSyncTaskStoreDO.class));
        verify(fullSyncExecutor, never()).submit(any());
    }

    @Test
    void createAllOpenStoresFullSync_shouldCreateTaskStoresFromOpenStoresThenSubmit() {
        EleStoreGoodsFullSyncAllOpenReqVO reqVO = new EleStoreGoodsFullSyncAllOpenReqVO();
        reqVO.setTestMode(false);
        when(taskMapper.selectRunningAllOpenStores()).thenReturn(null);
        when(storeService.getOpenPlatformStores(1L)).thenReturn(List.of(
                openStore("STORE001", "门店1", "ELE001", "MERCHANT001"),
                openStore("STORE002", "门店2", "ELE002", "MERCHANT002")
        ));
        doAnswer(invocation -> {
            EleStoreGoodsFullSyncTaskDO task = invocation.getArgument(0);
            task.setId(20L);
            return 1;
        }).when(taskMapper).insert(any(EleStoreGoodsFullSyncTaskDO.class));

        Long taskId = fullSyncService.createAllOpenStoresFullSync(reqVO);

        assertEquals(20L, taskId);
        ArgumentCaptor<EleStoreGoodsFullSyncTaskDO> taskCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        EleStoreGoodsFullSyncTaskDO task = taskCaptor.getValue();
        assertEquals("ALL_OPEN_STORES", task.getScope());
        assertEquals("PENDING", task.getStatus());
        assertEquals(false, task.getTestMode());
        assertEquals(2, task.getTotalStoreCount());

        ArgumentCaptor<EleStoreGoodsFullSyncTaskStoreDO> storeCaptor = ArgumentCaptor.forClass(EleStoreGoodsFullSyncTaskStoreDO.class);
        verify(taskStoreMapper, org.mockito.Mockito.times(2)).insert(storeCaptor.capture());
        List<EleStoreGoodsFullSyncTaskStoreDO> taskStores = storeCaptor.getAllValues();
        assertEquals("STORE001", taskStores.get(0).getStoreId());
        assertEquals("门店1", taskStores.get(0).getStoreName());
        assertEquals("ELE001", taskStores.get(0).getErpStoreCode());
        assertEquals("ELE001", taskStores.get(0).getPlatformStoreId());
        assertEquals("MERCHANT001", taskStores.get(0).getMerchantCode());
        assertEquals("STORE002", taskStores.get(1).getStoreId());
        assertEquals("ELE002", taskStores.get(1).getErpStoreCode());
        verify(fullSyncExecutor).submit(20L);
    }

    private StorePlatformRespVO openStore(String storeId, String storeName, String platformStoreId, String settlementAccount) {
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId(storeId);
        store.setPlatformStoreName(storeName);
        store.setPlatformStoreId(platformStoreId);
        store.setSettlementAccount(settlementAccount);
        return store;
    }
}
