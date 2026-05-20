package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreStockDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryShadowUpsertReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EleStoreInventoryIngestServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryIngestServiceImpl ingestService;

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

    @Test
    public void testIngest_whenFormalStockMatched_thenPersistFormal() {
        EleStoreInventoryIngestRowBO row = buildRow();
        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(11L);
        StoreProductDO storeProduct = new StoreProductDO();
        storeProduct.setStoreProductId("store-product-1");

        when(skuTableMapper.selectByProductSkuCode("SKU-1")).thenReturn(sku);
        when(storeProductMapper.selectByStoreIdAndProductSkuId("store-1", "11")).thenReturn(storeProduct);
        when(storeStockMapper.selectByStoreProductId("store-product-1")).thenReturn(null);
        when(storeStockMapper.insert(any(StoreStockDO.class))).thenReturn(1);

        EleStoreInventoryIngestResultBO result = ingestService.ingest(row);

        assertEquals(EleStoreInventoryIngestService.PERSIST_STATUS_FORMAL, result.getPersistStatus());
        assertNull(result.getReasonCode());
        assertNull(result.getShadowId());
        assertNull(result.getGovernanceId());
        verify(storeStockMapper).insert(argThat((StoreStockDO stock) ->
                "store-product-1".equals(stock.getStoreProductId())
                        && Integer.valueOf(20).equals(stock.getStoreStockQuantity())
                        && Integer.valueOf(18).equals(stock.getStoreStockAvailableQuantity())
                        && Integer.valueOf(1).equals(stock.getStoreStockTransitQuantity())
                        && Integer.valueOf(2).equals(stock.getStoreStockFrozenQuantity())
                        && assertStoreStockId(stock.getStoreStockId())));
        verifyNoInteractions(shadowService, governanceService);
    }

    @Test
    public void testIngest_whenSkuNotMatched_thenPersistShadowAndGovernance() {
        EleStoreInventoryIngestRowBO row = buildRow();
        EleStoreInventoryShadowDO shadow = new EleStoreInventoryShadowDO();
        shadow.setId(88L);

        when(skuTableMapper.selectByProductSkuCode("SKU-1")).thenReturn(null);
        when(shadowService.upsert(any(EleSkuInventoryShadowUpsertReqBO.class),
                eq(EleStoreInventoryIngestService.MATCH_STATUS_SKU_NOT_MATCHED),
                eq(EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND))).thenReturn(shadow);
        when(governanceService.createOrRefresh(any())).thenReturn(99L);

        EleStoreInventoryIngestResultBO result = ingestService.ingest(row);

        assertEquals(EleStoreInventoryIngestService.PERSIST_STATUS_SHADOW, result.getPersistStatus());
        assertEquals(EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND, result.getReasonCode());
        assertEquals(88L, result.getShadowId());
        assertEquals(99L, result.getGovernanceId());
        verify(shadowService).upsert(argThat(req ->
                        Integer.valueOf(18).equals(req.getAvailableForSale())
                                && Integer.valueOf(20).equals(req.getPhysicalStockTotalAmount())
                                && "raw-payload".equals(req.getRawPayload())),
                eq(EleStoreInventoryIngestService.MATCH_STATUS_SKU_NOT_MATCHED),
                eq(EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND));
        verify(governanceService).createOrRefresh(argThat(pool ->
                Long.valueOf(88L).equals(pool.getInventoryShadowId())
                        && "SKU-1".equals(pool.getSkuCode())
                        && EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND.equals(pool.getReasonCode())));
        verify(storeStockMapper, never()).insert(any(StoreStockDO.class));
        verify(storeStockMapper, never()).updateById(any(StoreStockDO.class));
    }

    @Test
    public void testIngest_whenStoreIdMissing_thenSkipFormalAndPersistShadow() {
        EleStoreInventoryIngestRowBO row = buildRow();
        row.setStoreId(" ");
        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(11L);
        EleStoreInventoryShadowDO shadow = new EleStoreInventoryShadowDO();
        shadow.setId(188L);

        when(skuTableMapper.selectByProductSkuCode("SKU-1")).thenReturn(sku);
        when(shadowService.upsert(any(EleSkuInventoryShadowUpsertReqBO.class),
                eq(EleStoreInventoryIngestService.MATCH_STATUS_SKU_NOT_MATCHED),
                eq(EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND))).thenReturn(shadow);
        when(governanceService.createOrRefresh(any())).thenReturn(199L);

        EleStoreInventoryIngestResultBO result = ingestService.ingest(row);

        assertEquals(EleStoreInventoryIngestService.PERSIST_STATUS_SHADOW, result.getPersistStatus());
        assertEquals(EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND, result.getReasonCode());
        assertEquals(188L, result.getShadowId());
        assertEquals(199L, result.getGovernanceId());
        verify(storeProductMapper, never()).selectByStoreIdAndProductSkuId(any(), any());
        verify(storeStockMapper, never()).insert(any(StoreStockDO.class));
        verify(storeStockMapper, never()).updateById(any(StoreStockDO.class));
        verify(shadowService).upsert(argThat(req -> req.getStoreId() == null),
                eq(EleStoreInventoryIngestService.MATCH_STATUS_SKU_NOT_MATCHED),
                eq(EleStoreInventoryIngestService.REASON_CODE_SKU_NOT_FOUND));
    }

    private EleStoreInventoryIngestRowBO buildRow() {
        EleStoreInventoryIngestRowBO row = new EleStoreInventoryIngestRowBO();
        row.setPlatformId(1L);
        row.setMerchantCode("merchant-1");
        row.setErpStoreCode("erp-store-1");
        row.setPlatformStoreId("platform-store-1");
        row.setStoreId("store-1");
        row.setSkuCode("SKU-1");
        row.setSubSkuCode("SUB-1");
        row.setAvailableForSale(18);
        row.setReservedAmount(2);
        row.setPhysicalStockTotalAmount(20);
        row.setPhysicalStockAvailableAmount(18);
        row.setPhysicalStockOccupiedAmount(1);
        row.setPhysicalStockIntransitAmount(1);
        row.setOwnerCode("owner-1");
        row.setOwnerName("owner-name-1");
        row.setRawPayload("raw-payload");
        return row;
    }

    private boolean assertStoreStockId(String storeStockId) {
        assertNotNull(storeStockId);
        return true;
    }
}
