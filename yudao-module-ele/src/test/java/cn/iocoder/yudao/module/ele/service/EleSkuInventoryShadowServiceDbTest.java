package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryShadowMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(EleSkuInventoryShadowServiceImpl.class)
@TestPropertySource(properties = "yudao.info.base-package=cn.iocoder.yudao")
class EleSkuInventoryShadowServiceDbTest extends BaseDbUnitTest {

    @Resource
    private EleSkuInventoryShadowService shadowService;
    @Resource
    private EleStoreInventoryShadowMapper shadowMapper;

    @Test
    void getByBizKey_whenSkuAndSubSkuHitDifferentRows_shouldNotRetireAnyRowOnRead() {
        Long skuRowId = insertShadow("SKU-1", "SUB-A");
        Long subSkuRowId = insertShadow("SKU-A", "SUB-1");

        EleStoreInventoryShadowDO result = shadowService.getByBizKey(1L, "merchant-1", "erp-store-1", "SKU-1", "SUB-1");

        assertEquals(skuRowId, result.getId());
        assertNotNull(shadowMapper.selectById(skuRowId));
        assertNotNull(shadowMapper.selectById(subSkuRowId));
    }

    private Long insertShadow(String skuCode, String subSkuCode) {
        EleStoreInventoryShadowDO shadow = new EleStoreInventoryShadowDO();
        shadow.setPlatformId(1L);
        shadow.setMerchantCode("merchant-1");
        shadow.setErpStoreCode("erp-store-1");
        shadow.setPlatformStoreId("platform-store-1");
        shadow.setStoreId("store-1");
        shadow.setSkuCode(skuCode);
        shadow.setSubSkuCode(subSkuCode);
        shadow.setAvailableForSale(10);
        shadow.setReservedAmount(1);
        shadow.setPhysicalStockTotalAmount(11);
        shadow.setPhysicalStockAvailableAmount(10);
        shadow.setPhysicalStockOccupiedAmount(1);
        shadow.setPhysicalStockIntransitAmount(0);
        shadow.setOwnerCode("owner-1");
        shadow.setOwnerName("owner-1");
        shadow.setMatchStatus("MATCHED");
        shadow.setReasonCode("OK");
        shadow.setReasonMsg("ok");
        shadow.setRawPayload("payload");
        shadow.setLastQueryTime(LocalDateTime.now());
        shadow.setUniqueDeleted(0L);
        shadow.setTenantId(1L);
        shadowMapper.insert(shadow);
        return shadow.getId();
    }
}
