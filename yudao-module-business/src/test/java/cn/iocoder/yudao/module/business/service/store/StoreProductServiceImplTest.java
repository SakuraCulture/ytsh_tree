package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class StoreProductServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private StoreProductServiceImpl storeProductService;

    @Mock
    private StoreProductMapper storeProductMapper;
    @Mock
    private StoreStockMapper storeStockMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private StoreProductShadowQueryService shadowQueryService;

    @Test
    void getStoreProductPage_shouldAppendShadowRows() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        StoreProductDO formal = new StoreProductDO();
        formal.setStoreProductId("SP001");
        formal.setStoreId("STORE001");
        formal.setProductSkuId("1001");
        formal.setStoreProductOwnership("入店");
        formal.setStoreProductPosStatus("1");
        formal.setStoreProductPrice(new BigDecimal("10.00"));
        formal.setStoreProductIsActive(1);
        formal.setCreateTime(LocalDateTime.of(2026, 4, 27, 10, 0));

        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(1001L);
        sku.setProductSkuCode("SKU001");
        sku.setProductSkuName("正式商品");

        StoreDO store = new StoreDO();
        store.setStoreId("STORE001");
        store.setStoreName("示例门店");

        when(storeProductMapper.selectCountForPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any())).thenReturn(1L);
        when(storeProductMapper.selectListForPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(formal));
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1L);
        when(skuTableMapper.selectListByProductSkuIds(anyCollection())).thenReturn(List.of(sku));
        when(storeMapper.selectList(any())).thenReturn(List.of(store));

        StoreProductShadowRowBO shadow = new StoreProductShadowRowBO();
        shadow.setShadowId(20L);
        shadow.setStoreId("STORE001");
        shadow.setPlatformStoreId("ELE_STORE_001");
        shadow.setSkuCode("SKU_MISSING");
        shadow.setProductName("缺主档商品");
        shadow.setSpuCode("SPU_MISSING");
        shadow.setSpecification("默认规格");
        shadow.setPrice(new BigDecimal("12.30"));
        shadow.setPosStatus(1);
        shadow.setIsActive(1);
        shadow.setMatchStatus("UNMATCHED");
        shadow.setCreateTime(LocalDateTime.of(2026, 4, 27, 11, 0));
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1L);
        when(shadowQueryService.listActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(shadow));

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(2, result.getList().size());
        assertEquals("FORMAL", result.getList().get(0).getRowSource());
        assertEquals("COMPLETE", result.getList().get(0).getCompletenessStatus());
        assertEquals("SHADOW", result.getList().get(1).getRowSource());
        assertEquals("MASTER_MISSING", result.getList().get(1).getCompletenessStatus());
        assertEquals("UNMATCHED", result.getList().get(1).getMatchStatus());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void getStoreProductPage_whenRowSourceShadow_shouldReturnShadowRowsEvenIfFormalEmpty() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setRowSource("SHADOW");

        StoreProductShadowRowBO shadow = new StoreProductShadowRowBO();
        shadow.setShadowId(21L);
        shadow.setStoreId("STORE002");
        shadow.setPlatformStoreId("ELE_STORE_002");
        shadow.setSkuCode("SKU_SHADOW_ONLY");
        shadow.setProductName("仅影子商品");
        shadow.setSpuCode("SPU_SHADOW_ONLY");
        shadow.setSpecification("大杯");
        shadow.setPrice(new BigDecimal("15.80"));
        shadow.setPosStatus(0);
        shadow.setIsActive(1);
        shadow.setMatchStatus("CONFLICT");
        shadow.setCreateTime(LocalDateTime.of(2026, 4, 27, 12, 0));
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1L);
        when(shadowQueryService.listActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(shadow));

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(1, result.getList().size());
        assertEquals("SHADOW", result.getList().get(0).getRowSource());
        assertEquals("MASTER_MISSING", result.getList().get(0).getCompletenessStatus());
        assertEquals("CONFLICT", result.getList().get(0).getMatchStatus());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void getStoreProductPage_whenShadowOnlyAndSkuFilterMissesFormalSku_shouldStillQueryShadowRows() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setRowSource("SHADOW");
        reqVO.setSkuCode("THIRD_SKU");
        when(skuTableMapper.selectListByKeyword("THIRD_SKU", null)).thenReturn(Collections.emptyList());
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1L);

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(1L, result.getTotal());
        verify(shadowQueryService).listActiveShadowRows(any(), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10));
    }

    @Test
    void getStoreProductPage_whenSkuFilterMissesFormalSku_shouldOnlyQueryShadowRows() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSkuCode("THIRD_SKU");
        when(skuTableMapper.selectListByKeyword("THIRD_SKU", null)).thenReturn(Collections.emptyList());
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1L);

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(1L, result.getTotal());
        verify(storeProductMapper, never()).selectCountForPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any());
        verify(storeProductMapper, never()).selectListForPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
        verify(shadowQueryService).listActiveShadowRows(any(), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10));
    }

    @Test
    void getStoreProductPage_whenRowSourceFormal_shouldReturnOnlyFormalRows() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setRowSource("FORMAL");

        StoreProductDO formal = new StoreProductDO();
        formal.setStoreProductId("SP002");
        formal.setStoreId("STORE001");
        formal.setProductSkuId("1002");
        formal.setStoreProductOwnership("入店");
        formal.setStoreProductPosStatus("1");
        formal.setStoreProductPrice(new BigDecimal("20.00"));
        formal.setStoreProductIsActive(1);
        formal.setCreateTime(LocalDateTime.of(2026, 4, 27, 13, 0));

        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(1002L);
        sku.setProductSkuCode("SKU002");
        sku.setProductSkuName("正式商品2");

        when(storeProductMapper.selectPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any()))
                .thenReturn(new PageResult<>(List.of(formal), 1L));
        when(skuTableMapper.selectListByProductSkuIds(anyCollection())).thenReturn(List.of(sku));
        when(storeMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(1, result.getList().size());
        assertEquals("FORMAL", result.getList().get(0).getRowSource());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void getStoreProductPage_whenCompletenessStatusMasterMissing_shouldReturnOnlyShadowRows() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setCompletenessStatus("MASTER_MISSING");

        StoreProductShadowRowBO shadow = new StoreProductShadowRowBO();
        shadow.setShadowId(23L);
        shadow.setStoreId("STORE003");
        shadow.setPlatformStoreId("ELE_STORE_003");
        shadow.setSkuCode("SKU_MASTER_MISSING");
        shadow.setProductName("主档缺失商品");
        shadow.setSpuCode("SPU003");
        shadow.setSpecification("标准杯");
        shadow.setPrice(new BigDecimal("18.50"));
        shadow.setPosStatus(1);
        shadow.setIsActive(1);
        shadow.setMatchStatus("UNMATCHED");
        shadow.setCreateTime(LocalDateTime.of(2026, 4, 27, 14, 0));
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1L);
        when(shadowQueryService.listActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(shadow));

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(1, result.getList().size());
        assertEquals("SHADOW", result.getList().get(0).getRowSource());
        assertEquals("MASTER_MISSING", result.getList().get(0).getCompletenessStatus());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void getStoreProductPage_shouldNotLoadAllShadowRowsBeforePagination() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        when(storeProductMapper.selectCountForPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any())).thenReturn(0L);
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(20L);

        storeProductService.getStoreProductPage(reqVO);

        verify(shadowQueryService, never()).listActiveShadowRows(any(), any());
        verify(shadowQueryService, times(1)).listActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10));
    }

    @Test
    void getStoreProductPage_shouldPaginateMergedRowsAndFillShadowStoreName() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(2);
        reqVO.setPageSize(20);

        List<StoreProductDO> formalList = new ArrayList<>();
        List<SkuTableDO> skuList = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            StoreProductDO formal = new StoreProductDO();
            formal.setStoreProductId(String.format("SP%03d", i));
            formal.setStoreId("STORE001");
            formal.setProductSkuId(String.valueOf(1000 + i));
            formal.setStoreProductOwnership("入店");
            formal.setStoreProductPosStatus("1");
            formal.setStoreProductPrice(new BigDecimal("10.00"));
            formal.setStoreProductIsActive(1);
            formal.setCreateTime(LocalDateTime.of(2026, 4, 27, 10, 0).minusMinutes(i));
            formalList.add(formal);

            SkuTableDO sku = new SkuTableDO();
            sku.setProductSkuId(1000L + i);
            sku.setProductSkuCode(String.format("SKU%03d", i));
            sku.setProductSkuName(String.format("正式商品%03d", i));
            skuList.add(sku);
        }

        StoreProductShadowRowBO shadow1 = new StoreProductShadowRowBO();
        shadow1.setShadowId(30L);
        shadow1.setStoreId("STORE001");
        shadow1.setPlatformStoreId("ELE_STORE_001");
        shadow1.setSkuCode("SKU_SHADOW_001");
        shadow1.setProductName("影子商品1");
        shadow1.setPrice(new BigDecimal("12.30"));
        shadow1.setPosStatus(1);
        shadow1.setIsActive(1);
        shadow1.setMatchStatus("UNMATCHED");
        shadow1.setCreateTime(LocalDateTime.of(2026, 4, 27, 11, 0));

        StoreProductShadowRowBO shadow2 = new StoreProductShadowRowBO();
        shadow2.setShadowId(31L);
        shadow2.setStoreId("STORE001");
        shadow2.setPlatformStoreId("ELE_STORE_001");
        shadow2.setSkuCode("SKU_SHADOW_002");
        shadow2.setProductName("影子商品2");
        shadow2.setPrice(new BigDecimal("13.30"));
        shadow2.setPosStatus(1);
        shadow2.setIsActive(1);
        shadow2.setMatchStatus("UNMATCHED");
        shadow2.setCreateTime(LocalDateTime.of(2026, 4, 27, 11, 5));

        StoreProductShadowRowBO shadow3 = new StoreProductShadowRowBO();
        shadow3.setShadowId(32L);
        shadow3.setStoreId("STORE001");
        shadow3.setPlatformStoreId("ELE_STORE_001");
        shadow3.setSkuCode("SKU_SHADOW_003");
        shadow3.setProductName("影子商品3");
        shadow3.setPrice(new BigDecimal("14.30"));
        shadow3.setPosStatus(1);
        shadow3.setIsActive(1);
        shadow3.setMatchStatus("UNMATCHED");
        shadow3.setCreateTime(LocalDateTime.of(2026, 4, 27, 11, 10));

        StoreDO store = new StoreDO();
        store.setStoreId("STORE001");
        store.setStoreName("示例门店");

        when(storeProductMapper.selectCountForPage(any(), org.mockito.ArgumentMatchers.<java.util.List<String>>any())).thenReturn(20L);
        when(shadowQueryService.countActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(3L);
        when(storeMapper.selectList(any())).thenReturn(List.of(store));
        when(shadowQueryService.listActiveShadowRows(any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(shadow1, shadow2, shadow3));

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(23L, result.getTotal());
        assertEquals(3, result.getList().size());
        assertEquals("SHADOW", result.getList().get(0).getRowSource());
        assertEquals("示例门店", result.getList().get(0).getStoreName());
        assertEquals("SKU_SHADOW_001", result.getList().get(0).getSkuCode());
        assertEquals("SKU_SHADOW_003", result.getList().get(2).getSkuCode());
    }
}
