package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryBatchQueryReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidator;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import lib.ele.retail.param.ErpSkuInventoryResultDTO;
import lib.ele.retail.param.MeEleRetailSaasSkuStockInventoryBatchQueryResDto;
import lib.ele.retail.param.SaasSkuStockInventoryBatchQueryResult;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleSkuInventoryQueryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleSkuInventoryQueryServiceImpl queryService;

    @Mock
    private StoreService storeService;
    @Mock
    private EleApiRateLimiter eleApiRateLimiter;
    @Mock
    private EleApiConfigMapper eleApiConfigMapper;
    @Mock
    private EleOpenApiClient eleOpenApiClient;
    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private StoreProductMapper storeProductMapper;
    @Mock
    private StoreStockMapper storeStockMapper;
    @Mock
    private EleSkuInventoryShadowService shadowService;
    @Mock
    private EleSkuInventoryGovernanceService governanceService;
    @Mock
    private EleStoreInventoryIngestService inventoryIngestService;
    @Mock
    private StoreIdentityValidator storeIdentityValidator;

    @Test
    void testQueryBatch_whenPlatformAndMerchantConflict_thenThrowIllegalArgumentException() {
        EleSkuInventoryBatchQueryReqBO reqBO = new EleSkuInventoryBatchQueryReqBO();
        reqBO.setPlatformStoreId("platform-store-1");
        reqBO.setMerchantCode("merchant-request");
        reqBO.setErpStoreCode("erp-store-1");
        reqBO.setSkuCodes(java.util.List.of("SKU-1"));

        StorePlatformRespVO storePlatform = buildStorePlatform("store-1", "platform-store-1", "merchant-local");
        when(storeService.getPlatformTableByPlatformStoreId("platform-store-1")).thenReturn(storePlatform);
        when(storeIdentityValidator.validate(eq("platform-store-1"), eq("merchant-request"), eq("erp-store-1"), eq(null), any(), eq(null), eq(null)))
                .thenReturn(cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidationResult.reject(
                        StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH,
                        "platform-store-1", "merchant-request", "erp-store-1", "store-1"));

        assertThrows(IllegalArgumentException.class, () -> queryService.queryBatch(reqBO));

        verify(eleApiRateLimiter, never()).acquirePermit(any());
        verify(eleOpenApiClient, never()).sendSkuStockInventoryBatchQuery(any(), any(), any(), any(), any());
    }

    @Test
    void testQueryBatch_whenInventoryRowMissing_thenIncreaseMissingAndFailureCount() {
        EleSkuInventoryBatchQueryReqBO reqBO = new EleSkuInventoryBatchQueryReqBO();
        reqBO.setPlatformStoreId("platform-store-1");
        reqBO.setSkuCodes(java.util.List.of("SKU-1", "SKU-2"));

        StorePlatformRespVO storePlatform = buildStorePlatform("store-1", "platform-store-1", "merchant-1");
        when(storeService.getPlatformTableByPlatformStoreId("platform-store-1")).thenReturn(storePlatform);
        when(storeIdentityValidator.validate(eq("platform-store-1"), eq(null), eq(null), eq(null), any(), eq(null), eq(null)))
                .thenReturn(cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidationResult.backfill(
                        "platform-store-1", "merchant-1", "platform-store-1", "store-1"));
        when(eleApiConfigMapper.selectActive()).thenReturn(new EleApiConfig());
        BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper = buildSuccessWrapper();
        when(eleOpenApiClient.sendSkuStockInventoryBatchQuery(any(), any(), eq("merchant-1"), eq("platform-store-1"), eq("platform-store-1")))
                .thenReturn(wrapper);

        EleStoreInventoryIngestResultBO ingestResult = new EleStoreInventoryIngestResultBO();
        ingestResult.setPersistStatus(EleStoreInventoryIngestService.PERSIST_STATUS_FORMAL);
        when(inventoryIngestService.ingest(any())).thenReturn(ingestResult);

        EleSkuInventoryBatchQueryRespDTO result = queryService.queryBatch(reqBO);

        assertEquals(1, result.getFormalSuccessCount());
        assertEquals(1, result.getMissingRowCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("PARTIAL_SUCCESS", result.getStatus());
        assertEquals(1, result.getErrorDetails().size());
        assertEquals("INVENTORY_ROW_MISSING:SKU:SKU-2", result.getErrorDetails().get(0));
    }

    @Test
    void testQueryBatch_whenDuplicateInventoryRow_thenIncreaseFailureCountAndKeepPartialSuccess() {
        EleSkuInventoryBatchQueryReqBO reqBO = new EleSkuInventoryBatchQueryReqBO();
        reqBO.setPlatformStoreId("platform-store-1");
        reqBO.setSkuCodes(java.util.List.of("SKU-1"));

        StorePlatformRespVO storePlatform = buildStorePlatform("store-1", "platform-store-1", "merchant-1");
        when(storeService.getPlatformTableByPlatformStoreId("platform-store-1")).thenReturn(storePlatform);
        when(storeIdentityValidator.validate(eq("platform-store-1"), eq(null), eq(null), eq(null), any(), eq(null), eq(null)))
                .thenReturn(cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidationResult.backfill(
                        "platform-store-1", "merchant-1", "platform-store-1", "store-1"));
        when(eleApiConfigMapper.selectActive()).thenReturn(new EleApiConfig());
        BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper = buildSuccessWrapper(
                buildInventory("SKU-1"),
                buildInventory("SKU-1"));
        when(eleOpenApiClient.sendSkuStockInventoryBatchQuery(any(), any(), eq("merchant-1"), eq("platform-store-1"), eq("platform-store-1")))
                .thenReturn(wrapper);

        EleStoreInventoryIngestResultBO ingestResult = new EleStoreInventoryIngestResultBO();
        ingestResult.setPersistStatus(EleStoreInventoryIngestService.PERSIST_STATUS_FORMAL);
        when(inventoryIngestService.ingest(any())).thenReturn(ingestResult);

        EleSkuInventoryBatchQueryRespDTO result = queryService.queryBatch(reqBO);

        assertEquals(1, result.getFormalSuccessCount());
        assertEquals(0, result.getMissingRowCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("PARTIAL_SUCCESS", result.getStatus());
        assertEquals(1, result.getErrorDetails().size());
        assertEquals("INVENTORY_DUPLICATE_ROW:SKU:SKU-1", result.getErrorDetails().get(0));
    }

    private StorePlatformRespVO buildStorePlatform(String storeId, String platformStoreId, String merchantCode) {
        StorePlatformRespVO storePlatform = new StorePlatformRespVO();
        storePlatform.setStoreId(storeId);
        storePlatform.setPlatformId(1L);
        storePlatform.setPlatformStoreId(platformStoreId);
        storePlatform.setSettlementAccount(merchantCode);
        return storePlatform;
    }

    @SuppressWarnings("unchecked")
    private BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> buildSuccessWrapper(ErpSkuInventoryResultDTO... inventories) {
        BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper = mock(BizResultWrapper.class);
        SaasSkuStockInventoryBatchQueryResult result = mock(SaasSkuStockInventoryBatchQueryResult.class);
        MeEleRetailSaasSkuStockInventoryBatchQueryResDto data = mock(MeEleRetailSaasSkuStockInventoryBatchQueryResDto.class);

        when(wrapper.getBody()).thenReturn(result);
        when(result.getErrno()).thenReturn("0");
        when(result.getData()).thenReturn(data);
        when(data.getInventory_list()).thenReturn(inventories);
        return wrapper;
    }

    private BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> buildSuccessWrapper() {
        return buildSuccessWrapper(buildInventory("SKU-1"));
    }

    private ErpSkuInventoryResultDTO buildInventory(String skuCode) {
        ErpSkuInventoryResultDTO inventory = new ErpSkuInventoryResultDTO();
        inventory.setSku_code(skuCode);
        inventory.setAvailable_for_sale(8);
        inventory.setReserved_amount(1);
        inventory.setPhysical_stock_total_amount(10);
        inventory.setPhysical_stock_available_amount(8);
        inventory.setPhysical_stock_occupied_amount(1);
        inventory.setPhysical_stock_intransit_amount(1);
        return inventory;
    }
}
