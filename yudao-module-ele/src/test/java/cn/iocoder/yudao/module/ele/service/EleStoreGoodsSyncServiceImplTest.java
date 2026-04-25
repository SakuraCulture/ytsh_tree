package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.service.store.StoreProductSyncWriteService;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.EleStoreGoodsQueryRespDTO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreGoodsSyncServiceImplTest extends BaseMockitoUnitTest {

    @Spy
    @InjectMocks
    private EleStoreGoodsSyncServiceImpl syncService;

    @Mock
    private StoreService storeService;
    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private StoreProductSyncWriteService storeProductSyncWriteService;
    @Mock
    private EleStoreGoodsGovernanceService governanceService;
    @Mock
    private EleStoreGoodsSyncLogService syncLogService;
    @Mock
    private ConfigApi configApi;
    @Mock
    private EleApiConfigMapper eleApiConfigMapper;
    @Mock
    private EleOpenApiClient eleOpenApiClient;

    @Test
    void syncStoreGoodsPage_shouldReturnPageSummaryWhenNoGoods() {
        EleStoreGoodsQueryReqBO reqBO = queryReqBO();
        reqBO.setPageNo(2);
        reqBO.setPageSize(20);
        EleStoreGoodsQueryRespDTO queryResp = new EleStoreGoodsQueryRespDTO();
        queryResp.setPage(2);
        queryResp.setPageSize(20);
        queryResp.setTotal(35);
        queryResp.setGoodsList(Collections.emptyList());
        doReturn(queryResp).when(syncService).queryStoreGoods(reqBO);

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(2, result.getPageNo());
        assertEquals(20, result.getPageSize());
        assertEquals(35, result.getTotal());
        assertEquals(0, result.getSyncCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals(0, result.getGovernanceCount());
    }

    @Test
    void syncStoreGoodsPage_shouldCountGovernanceWhenSkuMissing() {
        EleStoreGoodsQueryReqBO reqBO = queryReqBO();
        EleStoreGoodsQueryRespDTO queryResp = new EleStoreGoodsQueryRespDTO();
        queryResp.setMerchantCode("MERCHANT001");
        queryResp.setStoreCode("STORE001");
        queryResp.setPage(1);
        queryResp.setPageSize(20);
        queryResp.setTotal(1);
        EleStoreGoodsQueryRespDTO.GoodsItem goodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
        goodsItem.setMerchantCode("MERCHANT001");
        goodsItem.setStoreCode("STORE001");
        goodsItem.setSpuCode("SPU001");
        EleStoreGoodsQueryRespDTO.SkuItem skuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
        skuItem.setSkuCode("SKU001");
        goodsItem.setSkuList(List.of(skuItem));
        queryResp.setGoodsList(List.of(goodsItem));
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("STORE001");
        store.setPlatformStoreId("STORE001");
        doReturn(queryResp).when(syncService).queryStoreGoods(reqBO);
        when(storeService.getPlatformTableListByPlatformStoreId(1L, "STORE001")).thenReturn(List.of(store));
        when(skuTableMapper.selectByProductSkuCode("SKU001")).thenReturn(null);

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(1, result.getSyncCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(1, result.getGovernanceCount());
        verify(governanceService).create(org.mockito.ArgumentMatchers.any());
        verify(syncLogService).create(org.mockito.ArgumentMatchers.any());
    }

    private EleStoreGoodsQueryReqBO queryReqBO() {
        EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO();
        reqBO.setMerchantCode("MERCHANT001");
        reqBO.setErpStoreCode("STORE001");
        return reqBO;
    }
}

