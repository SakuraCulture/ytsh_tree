package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryShadowMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EleStoreInventorySkuScopeServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventorySkuScopeServiceImpl skuScopeService;

    @Mock
    private StoreProductMapper storeProductMapper;
    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private EleStoreGoodsShadowMapper storeGoodsShadowMapper;
    @Mock
    private EleStoreInventoryShadowMapper inventoryShadowMapper;

    @Test
    public void testListStoreSkuScope_whenAllSourcesPresent_thenMergeAndDeduplicateInOrder() {
        StoreProductDO firstStoreProduct = new StoreProductDO();
        firstStoreProduct.setProductSkuId("sku-id-1");
        StoreProductDO secondStoreProduct = new StoreProductDO();
        secondStoreProduct.setProductSkuId("sku-id-2");
        StoreProductDO blankStoreProduct = new StoreProductDO();
        blankStoreProduct.setProductSkuId(" ");
        SkuTableDO firstSku = new SkuTableDO();
        firstSku.setProductSkuCode("SKU-A");
        SkuTableDO secondSku = new SkuTableDO();
        secondSku.setProductSkuCode("SKU-B");
        SkuTableDO blankSku = new SkuTableDO();
        blankSku.setProductSkuCode(" ");

        when(storeProductMapper.selectListByStoreId("store-1"))
                .thenReturn(List.of(firstStoreProduct, secondStoreProduct, blankStoreProduct));
        when(skuTableMapper.selectListByProductSkuIds(List.of("sku-id-1", "sku-id-2")))
                .thenReturn(List.of(firstSku, secondSku, blankSku));
        when(storeGoodsShadowMapper.selectActiveSkuCodesByErpStoreCode("erp-store-1"))
                .thenReturn(List.of("SKU-B", "SKU-C", " "));
        when(inventoryShadowMapper.selectActiveSkuCodes("store-1", "erp-store-1"))
                .thenReturn(Arrays.asList("SKU-A", "SKU-D", null));

        List<String> result = skuScopeService.listStoreSkuScope("store-1", "erp-store-1");

        assertEquals(List.of("SKU-A", "SKU-B", "SKU-C", "SKU-D"), result);
    }

    @Test
    public void testListStoreSkuScope_whenStoreIdBlank_thenSkipLocalStoreLookup() {
        when(storeGoodsShadowMapper.selectActiveSkuCodesByErpStoreCode("erp-store-1"))
                .thenReturn(List.of("SKU-C"));
        when(inventoryShadowMapper.selectActiveSkuCodes("", "erp-store-1"))
                .thenReturn(List.of("SKU-D"));

        List<String> result = skuScopeService.listStoreSkuScope("", "erp-store-1");

        assertEquals(List.of("SKU-C", "SKU-D"), result);
        verifyNoInteractions(storeProductMapper, skuTableMapper);
    }
}
