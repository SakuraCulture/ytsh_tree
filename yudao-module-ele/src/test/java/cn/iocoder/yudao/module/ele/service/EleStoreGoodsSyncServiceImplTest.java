package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.service.store.StoreProductSyncWriteService;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductSyncUpsertReqBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsSyncLogDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.EleStoreGoodsQueryRespDTO;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidationResult;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidator;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Spy
    private StoreIdentityValidator storeIdentityValidator = new StoreIdentityValidator();

    @Test
    void syncStoreGoodsPage_whenStoreIdentityMismatch_thenOnlyCountFailAndWriteMismatchLog() {
        EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO("merchant-1", "erp-store-request", null, 1, 20);
        doReturn(buildQueryResp()).when(syncService).queryStoreGoods(any(EleStoreGoodsQueryReqBO.class));
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        when(storeService.getPlatformTableListByPlatformStoreId(1L, "erp-store-upstream")).thenReturn(List.of(buildLocalStore("erp-store-local")));
        doReturn(StoreIdentityValidationResult.reject(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH,
                "erp-store-request", "merchant-1", "erp-store-request", "store-local-1"))
                .when(storeIdentityValidator).validate(any(), any(), any(), any(), any(), any(), any());

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(1, result.getSyncCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(0, result.getGovernanceCount());
        assertEquals(0, result.getShadowCount());
        verify(storeProductSyncWriteService, never()).upsertStoreProduct(any());
        verify(syncLogService).create(argThat((EleStoreGoodsSyncLogDO log) ->
                Boolean.FALSE.equals(log.getSuccess())
                        && "STORE_IDENTITY_MISMATCH".equals(log.getResultCode())));
        verifyNoInteractions(skuTableMapper, shadowService, governanceService);
    }

    @Test
    void syncStoreGoodsPage_whenRealIdentityValidationPasses_thenWriteFormalProductWithoutResettingFirstSeenFields() {
        EleStoreGoodsQueryReqBO reqBO = new EleStoreGoodsQueryReqBO("merchant-1", "erp-store-upstream", null, 1, 20);
        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(1L);
        doReturn(buildQueryResp()).when(syncService).queryStoreGoods(any(EleStoreGoodsQueryReqBO.class));
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        when(storeService.getPlatformTableListByPlatformStoreId(1L, "erp-store-upstream"))
                .thenReturn(List.of(buildLocalStore("erp-store-upstream")));
        when(skuTableMapper.selectByProductSkuCode("SKU-1")).thenReturn(sku);
        when(storeProductSyncWriteService.upsertStoreProduct(any())).thenReturn("store-product-1");

        EleStoreGoodsPageSyncResult result = syncService.syncStoreGoodsPage(reqBO, false);

        assertEquals(1, result.getSyncCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        verify(storeProductSyncWriteService).upsertStoreProduct(argThat((StoreProductSyncUpsertReqBO upsertReqBO) ->
                "store-local-1".equals(upsertReqBO.getStoreId())
                        && "1".equals(upsertReqBO.getProductSkuId())
                        && upsertReqBO.getStoreProductOwnership() == null
                        && upsertReqBO.getStoreProductFirstDate() == null
                        && upsertReqBO.getStoreProductShelfTime() == null
                        && new BigDecimal("12.34").compareTo(upsertReqBO.getStoreProductPrice()) == 0));
    }

    private StorePlatformRespVO buildLocalStore(String platformStoreId) {
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("store-local-1");
        store.setPlatformId(1L);
        store.setPlatformStoreId(platformStoreId);
        store.setSettlementAccount("merchant-1");
        store.setStatus(1);
        return store;
    }

    private EleStoreGoodsQueryRespDTO buildQueryResp() {
        EleStoreGoodsQueryRespDTO queryResp = new EleStoreGoodsQueryRespDTO();
        queryResp.setMerchantCode("merchant-1");
        queryResp.setStoreCode("erp-store-upstream");
        queryResp.setPage(1);
        queryResp.setPageSize(20);
        queryResp.setTotal(1);

        EleStoreGoodsQueryRespDTO.GoodsItem goodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
        goodsItem.setMerchantCode("merchant-1");
        goodsItem.setStoreCode("erp-store-upstream");
        goodsItem.setTitle("测试商品");
        goodsItem.setSpuCode("SPU-1");

        EleStoreGoodsQueryRespDTO.SkuItem skuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
        skuItem.setSkuCode("SKU-1");
        skuItem.setSubSkuCode("SUB-1");
        skuItem.setSpecification("默认规格");
        skuItem.setSalePrice(1234L);
        skuItem.setStatus(1);
        goodsItem.setSkuList(List.of(skuItem));

        queryResp.setGoodsList(List.of(goodsItem));
        return queryResp;
    }
}
