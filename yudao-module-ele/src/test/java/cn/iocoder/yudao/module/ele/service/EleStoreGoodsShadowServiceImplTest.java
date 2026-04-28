package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.service.store.StoreProductSyncWriteService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreGoodsShadowServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreGoodsShadowServiceImpl shadowService;

    @Mock
    private EleStoreGoodsShadowMapper shadowMapper;
    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private StoreProductSyncWriteService storeProductSyncWriteService;

    @Test
    void upsertFromSync_shouldInsertUnmatchedShadowWhenMissing() {
        EleStoreGoodsShadowUpsertReqBO reqBO = buildReq();
        when(shadowMapper.selectByBizKey(1L, "MERCHANT001", "STORE001", "SKU001")).thenReturn(null);

        EleStoreGoodsShadowDO result = shadowService.upsertFromSync(reqBO, EleStoreGoodsShadowStatus.UNMATCHED, null, null);

        assertEquals("SKU001", result.getSkuCode());
        assertEquals(EleStoreGoodsShadowStatus.UNMATCHED, result.getMatchStatus());
        assertEquals(0L, result.getUniqueDeleted());
        assertNotNull(result.getLastSyncTime());
        assertNull(result.getMatchedProductSkuId());
        assertNull(result.getMergedStoreProductId());
        verify(shadowMapper).insert(any(EleStoreGoodsShadowDO.class));
        verify(shadowMapper, never()).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void upsertFromSync_shouldUpdateExistingWithExplicitWrapper() {
        EleStoreGoodsShadowUpsertReqBO reqBO = buildReq();
        EleStoreGoodsShadowDO existing = new EleStoreGoodsShadowDO();
        existing.setId(20L);
        existing.setTenantId(99L);
        existing.setCreator("creator");
        existing.setUniqueDeleted(0L);
        EleStoreGoodsShadowDO updated = new EleStoreGoodsShadowDO();
        updated.setId(20L);
        updated.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectByBizKey(1L, "MERCHANT001", "STORE001", "SKU001")).thenReturn(existing);
        when(shadowMapper.selectById(20L)).thenReturn(updated);

        EleStoreGoodsShadowDO result = shadowService.upsertFromSync(reqBO, EleStoreGoodsShadowStatus.UNMATCHED, null, null);

        assertEquals(20L, result.getId());
        assertEquals(EleStoreGoodsShadowStatus.UNMATCHED, result.getMatchStatus());
        ArgumentCaptor<EleStoreGoodsShadowDO> captor = ArgumentCaptor.forClass(EleStoreGoodsShadowDO.class);
        verify(shadowMapper).update(captor.capture(), any(Wrapper.class));
        assertNull(captor.getValue().getTenantId());
        assertNull(captor.getValue().getCreator());
        assertNull(captor.getValue().getUniqueDeleted());
        verify(shadowMapper, never()).insert(any(EleStoreGoodsShadowDO.class));
    }

    @Test
    void upsertFromSync_shouldRetryUpdateWhenInsertDuplicated() {
        EleStoreGoodsShadowUpsertReqBO reqBO = buildReq();
        EleStoreGoodsShadowDO existing = new EleStoreGoodsShadowDO();
        existing.setId(30L);
        when(shadowMapper.selectByBizKey(1L, "MERCHANT001", "STORE001", "SKU001"))
                .thenReturn(null)
                .thenReturn(existing);
        when(shadowMapper.insert(any(EleStoreGoodsShadowDO.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(shadowMapper.selectById(30L)).thenReturn(existing);

        EleStoreGoodsShadowDO result = shadowService.upsertFromSync(reqBO, EleStoreGoodsShadowStatus.UNMATCHED, null, null);

        assertEquals(30L, result.getId());
        verify(shadowMapper).insert(any(EleStoreGoodsShadowDO.class));
        verify(shadowMapper).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
        verify(shadowMapper, times(2)).selectByBizKey(1L, "MERCHANT001", "STORE001", "SKU001");
    }

    @Test
    void upsertFromSync_shouldRejectInvalidRequiredFields() {
        EleStoreGoodsShadowUpsertReqBO reqBO = buildReq();
        reqBO.setSkuCode(" ");

        assertThrows(IllegalArgumentException.class,
                () -> shadowService.upsertFromSync(reqBO, EleStoreGoodsShadowStatus.UNMATCHED, null, null));
        verify(shadowMapper, never()).insert(any(EleStoreGoodsShadowDO.class));
        verify(shadowMapper, never()).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void upsertFromSync_shouldRejectInvalidStatus() {
        EleStoreGoodsShadowUpsertReqBO reqBO = buildReq();

        assertThrows(IllegalArgumentException.class,
                () -> shadowService.upsertFromSync(reqBO, "BAD_STATUS", null, null));
        verify(shadowMapper, never()).insert(any(EleStoreGoodsShadowDO.class));
        verify(shadowMapper, never()).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void markMerged_shouldSetMergedStatusAndFormalIds() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(10L);
        shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectById(10L)).thenReturn(shadow);
        when(shadowMapper.update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class))).thenReturn(1);

        shadowService.markMerged(10L, " 1001 ", " SP0001 ");

        verify(shadowMapper).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void markMerged_shouldRejectBlankFormalIds() {
        assertThrows(IllegalArgumentException.class, () -> shadowService.markMerged(10L, " ", "SP0001"));
        assertThrows(IllegalArgumentException.class, () -> shadowService.markMerged(10L, "1001", " "));
        verify(shadowMapper, never()).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void getShadowPage_shouldDelegateMapperPage() {
        EleStoreGoodsShadowPageReqVO reqVO = new EleStoreGoodsShadowPageReqVO();
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(41L);
        when(shadowMapper.selectPage(any(EleStoreGoodsShadowPageReqVO.class))).thenReturn(new PageResult<>(List.of(shadow), 1L));

        PageResult<EleStoreGoodsShadowRespVO> result = shadowService.getShadowPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(41L, result.getList().get(0).getId());
    }

    @Test
    void getShadow_shouldReturnMappedResp() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(42L);
        shadow.setSkuCode("SKU042");
        when(shadowMapper.selectById(42L)).thenReturn(shadow);

        EleStoreGoodsShadowRespVO result = shadowService.getShadow(42L);

        assertEquals(42L, result.getId());
        assertEquals("SKU042", result.getSkuCode());
    }

    @Test
    void ignore_shouldMarkShadowIgnored() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(11L);
        shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectById(11L)).thenReturn(shadow);
        when(shadowMapper.update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class))).thenReturn(1);

        shadowService.ignore(11L);

        verify(shadowMapper).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void mergeManually_shouldUpsertFormalAndMarkMerged() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(12L);
        shadow.setStoreId("STORE001");
        shadow.setSkuCode("SKU001");
        shadow.setPosStatus("上架");
        shadow.setIsActive(1);
        shadow.setSalePrice(new BigDecimal("19.90"));
        shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectById(12L)).thenReturn(shadow);
        when(shadowMapper.update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class))).thenReturn(1);
        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(1001L);
        when(skuTableMapper.selectById(1001L)).thenReturn(sku);
        when(storeProductSyncWriteService.upsertStoreProduct(any())).thenReturn("SP0001");

        shadowService.mergeManually(12L, "1001");

        verify(storeProductSyncWriteService).upsertStoreProduct(any());
        verify(shadowMapper).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void mergeManually_shouldRejectMissingSku() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(13L);
        shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectById(13L)).thenReturn(shadow);
        when(skuTableMapper.selectById(1002L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> shadowService.mergeManually(13L, "1002"));
        verify(storeProductSyncWriteService, never()).upsertStoreProduct(any());
    }

    @Test
    void mergeManually_shouldNotMarkShadowWhenFormalUpsertFails() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(14L);
        shadow.setStoreId("STORE001");
        shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectById(14L)).thenReturn(shadow);
        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId(1003L);
        when(skuTableMapper.selectById(1003L)).thenReturn(sku);
        when(storeProductSyncWriteService.upsertStoreProduct(any())).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> shadowService.mergeManually(14L, "1003"));
        verify(shadowMapper, never()).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    private EleStoreGoodsShadowUpsertReqBO buildReq() {
        EleStoreGoodsShadowUpsertReqBO reqBO = new EleStoreGoodsShadowUpsertReqBO();
        reqBO.setPlatformId(1L);
        reqBO.setMerchantCode(" MERCHANT001 ");
        reqBO.setErpStoreCode(" STORE001 ");
        reqBO.setPlatformStoreId("STORE001");
        reqBO.setStoreId("LOCAL_STORE001");
        reqBO.setSpuCode("SPU001");
        reqBO.setSkuCode(" SKU001 ");
        reqBO.setSubSkuCode("SUB001");
        reqBO.setTitle("测试商品");
        reqBO.setMainPic("https://example.com/main.jpg");
        reqBO.setSubPics("[]");
        reqBO.setFrontCategory("[]");
        reqBO.setBrandName("测试品牌");
        reqBO.setSpecification("默认规格");
        reqBO.setSalePrice(new BigDecimal("12.30"));
        reqBO.setPosStatus("上架");
        reqBO.setIsActive(1);
        reqBO.setRawPayload("{skuCode=SKU001}");
        return reqBO;
    }
}
