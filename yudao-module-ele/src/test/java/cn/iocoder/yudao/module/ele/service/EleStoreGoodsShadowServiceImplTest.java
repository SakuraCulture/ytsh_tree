package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
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

        shadowService.markMerged(10L, " 1001 ", " SP0001 ");

        verify(shadowMapper).update(any(EleStoreGoodsShadowDO.class), any(Wrapper.class));
    }

    @Test
    void markMerged_shouldRejectBlankFormalIds() {
        assertThrows(IllegalArgumentException.class, () -> shadowService.markMerged(10L, " ", "SP0001"));
        assertThrows(IllegalArgumentException.class, () -> shadowService.markMerged(10L, "1001", " "));
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
