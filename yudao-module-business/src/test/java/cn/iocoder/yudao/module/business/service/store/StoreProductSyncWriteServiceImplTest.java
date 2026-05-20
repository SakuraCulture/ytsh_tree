package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductSyncUpsertReqBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreProductSyncWriteServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private StoreProductSyncWriteServiceImpl syncWriteService;

    @Mock
    private StoreProductMapper storeProductMapper;

    @Test
    void upsertStoreProduct_whenExistingRelationFound_thenUpdateByIdWithoutInsert() {
        StoreProductDO exist = new StoreProductDO();
        exist.setStoreProductId("store-product-1");
        exist.setStoreId("store-1");
        exist.setProductSkuId("sku-1");
        exist.setStoreProductOwnership("入店");
        exist.setStoreProductFirstDate(LocalDate.of(2026, 5, 1));
        exist.setStoreProductShelfTime(LocalDateTime.of(2026, 5, 1, 8, 0));
        when(storeProductMapper.selectByStoreIdAndProductSkuId("store-1", "sku-1")).thenReturn(exist);

        StoreProductSyncUpsertReqBO reqBO = new StoreProductSyncUpsertReqBO();
        reqBO.setStoreId("store-1");
        reqBO.setProductSkuId("sku-1");
        reqBO.setStoreProductPrice(new BigDecimal("12.34"));
        reqBO.setStoreProductIsActive(1);
        reqBO.setStoreProductPosStatus("上架");

        String result = syncWriteService.upsertStoreProduct(reqBO);

        assertEquals("store-product-1", result);
        verify(storeProductMapper).updateById(argThat((StoreProductDO updateObj) ->
                "store-product-1".equals(updateObj.getStoreProductId())
                        && "入店".equals(updateObj.getStoreProductOwnership())
                        && LocalDate.of(2026, 5, 1).equals(updateObj.getStoreProductFirstDate())
                        && LocalDateTime.of(2026, 5, 1, 8, 0).equals(updateObj.getStoreProductShelfTime())
                        && new BigDecimal("12.34").compareTo(updateObj.getStoreProductPrice()) == 0));
        verify(storeProductMapper, never()).insert(any(StoreProductDO.class));
    }
}
