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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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
    @Mock
    private EleStoreGoodsShadowService shadowService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private void mockTransactionTemplate() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction((TransactionStatus) null);
        }).when(transactionTemplate).execute(any(TransactionCallback.class));
    }

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
        mockTransactionTemplate();
        EleStoreGoodsQueryReqBO reqBO = queryReqBO();
        EleStoreGoodsQueryRespDTO queryResp = new EleStoreGoodsQueryRespDTO();
        queryResp.setMerchantCode("MERCHANT001");
        queryResp.setStoreCode("STORE001");
        queryResp.setPage(1);
        queryResp.setPageSize(20);
        queryResp.setTotal(1);
        EleStoreGoodsQueryRespDTO.GoodsItem goodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
        goodsItem.setMerchantCode("MERCHANT001");
        goodsItem.setStoreCode("LOCAL_STORE_001");
        goodsItem.setSpuCode("SPU001");
        goodsItem.setTitle("测试商品");
        goodsItem.setMainPic("https://img.example.com/main.png");
        EleStoreGoodsQueryRespDTO.SkuItem skuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
        skuItem.setSkuCode("SKU001");
        skuItem.setSpecification("默认规格");
        goodsItem.setSkuList(List.of(skuItem));
        queryResp.setGoodsList(List.of(goodsItem));
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("LOCAL_STORE_001");
        store.setPlatformStoreId("PLATFORM_STORE_001");
        store.setPlatformId(1L);
        store.setStatus(1);
        doReturn(queryResp).when(syncService).queryStoreGoods(reqBO);
        when(storeService.getPlatformTableListByStoreId("LOCAL_STORE_001")).thenReturn(List.of(store));
        when(skuTableMapper.selectByProductSkuCode("SKU001")).thenReturn(null);

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(1, result.getSyncCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals(1, result.getGovernanceCount());
        assertEquals(1, result.getShadowCount());
        verify(governanceService).create(org.mockito.ArgumentMatchers.any());
        verify(syncLogService).create(org.mockito.ArgumentMatchers.any());
        verify(storeProductSyncWriteService, never()).upsertStoreProduct(org.mockito.ArgumentMatchers.any());
        verify(shadowService).upsertFromSync(org.mockito.ArgumentMatchers.argThat(req ->
                        "MERCHANT001".equals(req.getMerchantCode())
                                && "STORE001".equals(req.getErpStoreCode())
                                && "PLATFORM_STORE_001".equals(req.getPlatformStoreId())
                                && "LOCAL_STORE_001".equals(req.getStoreId())
                                && "SPU001".equals(req.getSpuCode())
                                && "SKU001".equals(req.getSkuCode())
                                && "测试商品".equals(req.getTitle())
                                && "https://img.example.com/main.png".equals(req.getMainPic())
                                && "默认规格".equals(req.getSpecification())),
                org.mockito.ArgumentMatchers.eq("UNMATCHED"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void syncStoreGoodsPage_shouldWriteMergedShadowBeforeSuccessLogWhenSkuMatched() {
        mockTransactionTemplate();
        EleStoreGoodsQueryReqBO reqBO = queryReqBO();
        EleStoreGoodsQueryRespDTO queryResp = new EleStoreGoodsQueryRespDTO();
        queryResp.setMerchantCode("MERCHANT001");
        queryResp.setStoreCode("STORE001");
        queryResp.setPage(1);
        queryResp.setPageSize(20);
        queryResp.setTotal(1);
        EleStoreGoodsQueryRespDTO.GoodsItem goodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
        goodsItem.setMerchantCode("MERCHANT001");
        goodsItem.setStoreCode("LOCAL_STORE_001");
        goodsItem.setSpuCode("SPU001");
        goodsItem.setTitle("已匹配商品");
        goodsItem.setMainPic("https://img.example.com/matched.png");
        EleStoreGoodsQueryRespDTO.SkuItem skuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
        skuItem.setSkuCode("SKU001");
        skuItem.setSpecification("大杯");
        skuItem.setSalePrice(1230L);
        goodsItem.setSkuList(List.of(skuItem));
        queryResp.setGoodsList(List.of(goodsItem));
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("LOCAL_STORE_001");
        store.setPlatformStoreId("PLATFORM_STORE_001");
        store.setPlatformId(1L);
        store.setStatus(1);
        cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO sku =
                new cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO();
        sku.setProductSkuId(1001L);
        doReturn(queryResp).when(syncService).queryStoreGoods(reqBO);
        when(storeService.getPlatformTableListByStoreId("LOCAL_STORE_001")).thenReturn(List.of(store));
        when(skuTableMapper.selectByProductSkuCode("SKU001")).thenReturn(sku);
        when(storeProductSyncWriteService.upsertStoreProduct(org.mockito.ArgumentMatchers.any())).thenReturn("SP0001");

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(1, result.getSyncCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals(0, result.getGovernanceCount());
        assertEquals(0, result.getShadowCount());
        org.mockito.InOrder inOrder = inOrder(storeProductSyncWriteService, shadowService, syncLogService);
        inOrder.verify(storeProductSyncWriteService).upsertStoreProduct(org.mockito.ArgumentMatchers.any());
        inOrder.verify(shadowService).upsertFromSync(org.mockito.ArgumentMatchers.argThat(req ->
                        "MERCHANT001".equals(req.getMerchantCode())
                                && "STORE001".equals(req.getErpStoreCode())
                                && "PLATFORM_STORE_001".equals(req.getPlatformStoreId())
                                && "LOCAL_STORE_001".equals(req.getStoreId())
                                && "SPU001".equals(req.getSpuCode())
                                && "SKU001".equals(req.getSkuCode())
                                && "已匹配商品".equals(req.getTitle())
                                && "https://img.example.com/matched.png".equals(req.getMainPic())
                                && "大杯".equals(req.getSpecification())),
                org.mockito.ArgumentMatchers.eq("MERGED"),
                org.mockito.ArgumentMatchers.eq("1001"),
                org.mockito.ArgumentMatchers.eq("SP0001"));
        inOrder.verify(syncLogService).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void syncStoreGoodsPage_shouldContinueAfterSingleItemFailure() {
        mockTransactionTemplate();
        EleStoreGoodsQueryReqBO reqBO = queryReqBO();
        EleStoreGoodsQueryRespDTO queryResp = new EleStoreGoodsQueryRespDTO();
        queryResp.setMerchantCode("MERCHANT001");
        queryResp.setStoreCode("STORE001");
        queryResp.setPage(1);
        queryResp.setPageSize(20);
        queryResp.setTotal(2);

        EleStoreGoodsQueryRespDTO.GoodsItem failedGoodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
        failedGoodsItem.setMerchantCode("MERCHANT001");
        failedGoodsItem.setStoreCode("LOCAL_STORE_404");
        failedGoodsItem.setSpuCode("SPU001");
        failedGoodsItem.setTitle("失败商品");
        EleStoreGoodsQueryRespDTO.SkuItem failedSkuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
        failedSkuItem.setSkuCode("SKU001");
        failedSkuItem.setSpecification("缺失门店");
        failedGoodsItem.setSkuList(List.of(failedSkuItem));

        EleStoreGoodsQueryRespDTO.GoodsItem successGoodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
        successGoodsItem.setMerchantCode("MERCHANT001");
        successGoodsItem.setStoreCode("LOCAL_STORE_001");
        successGoodsItem.setSpuCode("SPU002");
        successGoodsItem.setTitle("成功商品");
        successGoodsItem.setMainPic("https://img.example.com/success.png");
        EleStoreGoodsQueryRespDTO.SkuItem successSkuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
        successSkuItem.setSkuCode("SKU002");
        successSkuItem.setSpecification("正常规格");
        successSkuItem.setSalePrice(2580L);
        successGoodsItem.setSkuList(List.of(successSkuItem));

        queryResp.setGoodsList(List.of(failedGoodsItem, successGoodsItem));

        StorePlatformRespVO successStore = new StorePlatformRespVO();
        successStore.setStoreId("LOCAL_STORE_001");
        successStore.setPlatformStoreId("PLATFORM_STORE_001");
        successStore.setPlatformId(1L);
        successStore.setStatus(1);
        cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO successSku =
                new cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO();
        successSku.setProductSkuId(1002L);

        doReturn(queryResp).when(syncService).queryStoreGoods(reqBO);
        when(storeService.getPlatformTableListByStoreId("LOCAL_STORE_404")).thenReturn(Collections.emptyList());
        when(storeService.getPlatformTableListByStoreId("LOCAL_STORE_001")).thenReturn(List.of(successStore));
        when(skuTableMapper.selectByProductSkuCode("SKU002")).thenReturn(successSku);
        when(storeProductSyncWriteService.upsertStoreProduct(org.mockito.ArgumentMatchers.any())).thenReturn("SP0002");

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(2, result.getSyncCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(0, result.getGovernanceCount());
        assertEquals(0, result.getShadowCount());
        verify(shadowService).upsertFromSync(org.mockito.ArgumentMatchers.argThat(req ->
                        "MERCHANT001".equals(req.getMerchantCode())
                                && "STORE001".equals(req.getErpStoreCode())
                                && "PLATFORM_STORE_001".equals(req.getPlatformStoreId())
                                && "LOCAL_STORE_001".equals(req.getStoreId())
                                && "SPU002".equals(req.getSpuCode())
                                && "SKU002".equals(req.getSkuCode())
                                && "成功商品".equals(req.getTitle())
                                && "https://img.example.com/success.png".equals(req.getMainPic())
                                && "正常规格".equals(req.getSpecification())),
                org.mockito.ArgumentMatchers.eq("MERGED"),
                org.mockito.ArgumentMatchers.eq("1002"),
                org.mockito.ArgumentMatchers.eq("SP0002"));
        verify(storeProductSyncWriteService).upsertStoreProduct(org.mockito.ArgumentMatchers.any());
    }

    private EleStoreGoodsQueryReqBO queryReqBO() {
        EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO();
        reqBO.setMerchantCode("MERCHANT001");
        reqBO.setErpStoreCode("STORE001");
        return reqBO;
    }
}

