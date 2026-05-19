package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(EleStoreInventorySkuScopeServiceImpl.class)
@TestPropertySource(properties = "yudao.info.base-package=cn.iocoder.yudao")
class EleStoreInventorySkuScopeServiceDbTest extends BaseDbUnitTest {

    @Resource
    private EleStoreInventorySkuScopeService skuScopeService;
    @Resource
    private EleStoreGoodsShadowMapper storeGoodsShadowMapper;

    @Test
    void listStoreSkuScope_whenGoodsShadowIsActiveButUnmatched_shouldExcludeSkuFromInventoryScope() {
        insertShadow("erp-store-1", "SKU-UNMATCHED", 1, EleStoreGoodsShadowStatus.UNMATCHED, 2);
        insertShadow("erp-store-1", "SKU-MATCHED", 1, EleStoreGoodsShadowStatus.MATCHED, 1);
        insertShadow("erp-store-1", "SKU-INACTIVE", 0, EleStoreGoodsShadowStatus.MATCHED, 0);

        List<String> result = skuScopeService.listStoreSkuScope("", "erp-store-1");

        assertEquals(List.of("SKU-MATCHED"), result);
    }

    private void insertShadow(String erpStoreCode, String skuCode, Integer isActive, String matchStatus, int minutesAgo) {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setPlatformId(1L);
        shadow.setMerchantCode("merchant-1");
        shadow.setErpStoreCode(erpStoreCode);
        shadow.setPlatformStoreId(erpStoreCode);
        shadow.setStoreId("store-1");
        shadow.setSpuCode("spu-" + skuCode);
        shadow.setSkuCode(skuCode);
        shadow.setSubSkuCode("sub-" + skuCode);
        shadow.setTitle("title-" + skuCode);
        shadow.setSpecification("spec-" + skuCode);
        shadow.setPosStatus("ENABLE");
        shadow.setIsActive(isActive);
        shadow.setMatchStatus(matchStatus);
        shadow.setTenantId(1L);
        shadow.setUpdateTime(LocalDateTime.now().minusMinutes(minutesAgo));
        storeGoodsShadowMapper.insert(shadow);
    }
}
