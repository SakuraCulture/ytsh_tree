package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportExcelVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportRespVO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EleStoreInventoryImportServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryImportServiceImpl importService;

    @Mock
    private StoreService storeService;
    @Mock
    private EleStoreInventoryIngestService ingestService;

    @Test
    public void testImportRows_whenFormalShadowAndInvalidMixed_thenCountByResult() {
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("store-1");
        store.setPlatformStoreId("erp-store-1");
        store.setSettlementAccount("merchant-1");
        when(storeService.getOpenPlatformStores(1L)).thenReturn(List.of(store));

        EleStoreInventoryIngestResultBO formal = new EleStoreInventoryIngestResultBO();
        formal.setPersistStatus(EleStoreInventoryIngestService.PERSIST_STATUS_FORMAL);
        EleStoreInventoryIngestResultBO shadow = new EleStoreInventoryIngestResultBO();
        shadow.setPersistStatus(EleStoreInventoryIngestService.PERSIST_STATUS_SHADOW);
        shadow.setGovernanceId(99L);
        when(ingestService.ingest(any())).thenReturn(formal, shadow);

        EleStoreInventoryImportRespVO result = importService.importRows(List.of(
                buildRow("erp-store-1", "SKU-1"),
                buildRow("erp-store-1", "SKU-2"),
                buildRow("erp-store-1", " ")
        ));

        assertEquals(1, result.getFormalSuccessCount());
        assertEquals(1, result.getShadowSuccessCount());
        assertEquals(1, result.getGovernanceCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureList().size());
        assertEquals(3, result.getFailureList().get(0).getRowNo());
        verify(ingestService).ingest(argThat(row -> "store-1".equals(row.getStoreId())
                && "merchant-1".equals(row.getMerchantCode())
                && "erp-store-1".equals(row.getPlatformStoreId())
                && "SKU-1".equals(row.getSkuCode())));
    }

    @Test
    public void testImportRows_whenStoreNotMatched_thenCountFailureWithoutIngest() {
        when(storeService.getOpenPlatformStores(1L)).thenReturn(List.of());

        EleStoreInventoryImportRespVO result = importService.importRows(List.of(buildRow("erp-store-404", "SKU-1")));

        assertEquals(0, result.getFormalSuccessCount());
        assertEquals(0, result.getShadowSuccessCount());
        assertEquals(0, result.getGovernanceCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureList().size());
        assertEquals("ERP门店编码未匹配到平台门店", result.getFailureList().get(0).getMessage());
        verify(ingestService, never()).ingest(any());
    }

    private EleStoreInventoryImportExcelVO buildRow(String erpStoreCode, String skuCode) {
        EleStoreInventoryImportExcelVO row = new EleStoreInventoryImportExcelVO();
        row.setErpStoreCode(erpStoreCode);
        row.setSkuCode(skuCode);
        row.setSubSkuCode("SUB-1");
        row.setPhysicalStockTotalAmount(10);
        row.setAvailableForSale(8);
        row.setReservedAmount(1);
        row.setPhysicalStockAvailableAmount(8);
        row.setPhysicalStockOccupiedAmount(1);
        row.setPhysicalStockIntransitAmount(1);
        row.setRemark("remark");
        return row;
    }
}
