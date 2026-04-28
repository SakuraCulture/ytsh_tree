package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreProductShadowQueryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private StoreProductShadowQueryServiceImpl shadowQueryService;

    @Mock
    private EleStoreGoodsShadowMapper eleStoreGoodsShadowMapper;

    @Test
    void countActiveShadowRows_whenTenantContextMissing_shouldPassNullTenantId() {
        TenantContextHolder.clear();
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setStoreId("STORE001");

        shadowQueryService.countActiveShadowRows(reqVO, true);

        verify(eleStoreGoodsShadowMapper).selectActiveCount(
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("STORE001"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void activeShadowPageSql_shouldNotReferenceTenantColumnsOnFormalTables() throws NoSuchMethodException {
        Method method = EleStoreGoodsShadowMapper.class.getMethod("selectActivePage", java.util.Collection.class,
                Long.class, String.class, String.class, String.class, String.class, boolean.class, int.class, int.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertFalse(sql.contains("sp.tenant_id"));
        assertFalse(sql.contains("sku.tenant_id"));
    }

    @Test
    void listActiveShadowRows_shouldKeepOtherStoreShadowWhenSkuCodeMatchesFormal() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();

        EleStoreGoodsShadowDO sameStoreShadow = new EleStoreGoodsShadowDO();
        sameStoreShadow.setId(1L);
        sameStoreShadow.setStoreId("STORE001");
        sameStoreShadow.setSkuCode("SKU001");
        sameStoreShadow.setTitle("同门店同SKU");
        sameStoreShadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);

        EleStoreGoodsShadowDO otherStoreShadow = new EleStoreGoodsShadowDO();
        otherStoreShadow.setId(2L);
        otherStoreShadow.setStoreId("STORE002");
        otherStoreShadow.setSkuCode("SKU001");
        otherStoreShadow.setTitle("跨门店同SKU");
        otherStoreShadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);

        when(eleStoreGoodsShadowMapper.selectActiveList(
                org.mockito.ArgumentMatchers.argThat(statuses -> statuses.contains(EleStoreGoodsShadowStatus.UNMATCHED)
                        && statuses.contains(EleStoreGoodsShadowStatus.CONFLICT)
                        && statuses.size() == 2),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull())).thenReturn(List.of(sameStoreShadow, otherStoreShadow));

        List<StoreProductShadowRowBO> result = shadowQueryService.listActiveShadowRows(reqVO, Set.of("STORE001#SKU001"));

        assertEquals(1, result.size());
        assertEquals("STORE002", result.get(0).getStoreId());
        assertEquals("SKU001", result.get(0).getSkuCode());
    }
}
