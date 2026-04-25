package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagVirtualDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagVirtualMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({TagVirtualServiceImpl.class, TagVirtualServiceImplTest.TestConfig.class})
class TagVirtualServiceImplTest extends BaseDbUnitTest {

    @Configuration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Resource
    private TagVirtualService tagVirtualService;

    @Resource
    private TagVirtualMapper tagVirtualMapper;

    @Test
    void createTagVirtual_shouldPersistValidJson() {
        Long id = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\",\"rules\":[{\"field\":\"price\",\"operator\":\">\",\"value\":100}]}"));

        TagVirtualDO tagVirtual = tagVirtualMapper.selectById(id);
        assertEquals("PRODUCT", tagVirtual.getDomainType());
        assertEquals("高价值商品", tagVirtual.getName());
        assertEquals("high_value_product", tagVirtual.getCode());
        assertEquals("{\"op\":\"and\",\"rules\":[{\"field\":\"price\",\"operator\":\">\",\"value\":100}]}", tagVirtual.getExpressionJson());
        assertEquals(STATUS_ENABLED, tagVirtual.getStatus());
        assertEquals(0L, tagVirtual.getUniqueDeleted());
    }

    @Test
    void createTagVirtual_shouldRejectInvalidJson() {
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品", "high_value_product", "not-json");

        assertServiceException(() -> tagVirtualService.createTagVirtual(reqVO), TAG_VIRTUAL_EXPRESSION_INVALID);
    }

    @Test
    void createTagVirtual_shouldRejectNonObjectJson() {
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品", "high_value_product", "[]");

        assertServiceException(() -> tagVirtualService.createTagVirtual(reqVO), TAG_VIRTUAL_EXPRESSION_INVALID);
    }

    @Test
    void createTagVirtual_shouldRejectInvalidStatus() {
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品", "high_value_product", "{\"op\":\"and\"}");
        reqVO.setStatus(2);

        assertServiceException(() -> tagVirtualService.createTagVirtual(reqVO), TAG_VIRTUAL_STATUS_INVALID);
    }

    @Test
    void createTagVirtual_shouldDefaultStatusToEnabledWhenNull() {
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品", "high_value_product", "{\"op\":\"and\"}");
        reqVO.setStatus(null);

        Long id = tagVirtualService.createTagVirtual(reqVO);

        TagVirtualDO created = tagVirtualMapper.selectById(id);
        assertEquals(STATUS_ENABLED, created.getStatus());
    }

    @Test
    void createTagVirtual_shouldRejectInvalidDomainType() {
        TagVirtualSaveReqVO reqVO = buildReq("UNKNOWN", "高价值商品", "high_value_product", "{\"op\":\"and\"}");

        assertServiceException(() -> tagVirtualService.createTagVirtual(reqVO), TAG_DOMAIN_TYPE_INVALID);
    }

    @Test
    void createTagVirtual_shouldRejectDuplicateCodeInDomain() {
        tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\"}"));

        assertServiceException(() -> tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品2", "high_value_product",
                        "{\"op\":\"or\"}")),
                TAG_VIRTUAL_CODE_EXISTS);
    }

    @Test
    void updateTagVirtual_shouldUpdateSuccessfully() {
        Long id = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\"}"));
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高潜商品", "potential_product",
                "{\"op\":\"or\",\"rules\":[{\"field\":\"price\",\"operator\":\">\",\"value\":50}]}"
        );
        reqVO.setId(id);
        reqVO.setStatus(STATUS_DISABLED);

        tagVirtualService.updateTagVirtual(reqVO);

        TagVirtualDO updated = tagVirtualMapper.selectById(id);
        assertEquals("高潜商品", updated.getName());
        assertEquals("potential_product", updated.getCode());
        assertEquals("{\"op\":\"or\",\"rules\":[{\"field\":\"price\",\"operator\":\">\",\"value\":50}]}", updated.getExpressionJson());
        assertEquals(STATUS_DISABLED, updated.getStatus());
    }

    @Test
    void updateTagVirtual_shouldRejectDuplicateCodeInSameDomain() {
        Long firstId = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\"}"));
        tagVirtualService.createTagVirtual(buildReq("PRODUCT", "潜力商品", "potential_product",
                "{\"op\":\"or\"}"));
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品-更新", "potential_product", "{\"op\":\"and\"}");
        reqVO.setId(firstId);

        assertServiceException(() -> tagVirtualService.updateTagVirtual(reqVO), TAG_VIRTUAL_CODE_EXISTS);
    }

    @Test
    void updateTagVirtual_shouldRejectInvalidJson() {
        Long id = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\"}"));
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品-更新", "high_value_product_2", "broken-json");
        reqVO.setId(id);

        assertServiceException(() -> tagVirtualService.updateTagVirtual(reqVO), TAG_VIRTUAL_EXPRESSION_INVALID);
    }

    @Test
    void updateTagVirtual_shouldRejectInvalidStatus() {
        Long id = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\"}"));
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品-更新", "high_value_product_2", "{\"op\":\"or\"}");
        reqVO.setId(id);
        reqVO.setStatus(2);

        assertServiceException(() -> tagVirtualService.updateTagVirtual(reqVO), TAG_VIRTUAL_STATUS_INVALID);
    }

    @Test
    void updateTagVirtual_whenNotExists_shouldThrow() {
        TagVirtualSaveReqVO reqVO = buildReq("PRODUCT", "高价值商品-更新", "high_value_product_2", "{\"op\":\"or\"}");
        reqVO.setId(999L);

        assertServiceException(() -> tagVirtualService.updateTagVirtual(reqVO), TAG_VIRTUAL_NOT_EXISTS);
    }

    @Test
    void deleteTagVirtual_whenNotExists_shouldThrow() {
        assertServiceException(() -> tagVirtualService.deleteTagVirtual(999L), TAG_VIRTUAL_NOT_EXISTS);
    }

    @Test
    void deleteTagVirtual_shouldReleaseUniqueDeleted() {
        Long firstId = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品", "high_value_product",
                "{\"op\":\"and\"}"));

        tagVirtualService.deleteTagVirtual(firstId);
        Long secondId = tagVirtualService.createTagVirtual(buildReq("PRODUCT", "高价值商品2", "high_value_product",
                "{\"op\":\"or\"}"));
        tagVirtualService.deleteTagVirtual(secondId);

        TagVirtualDO firstDeleted = tagVirtualMapper.selectByIdIgnoreDeleted(firstId);
        TagVirtualDO secondDeleted = tagVirtualMapper.selectByIdIgnoreDeleted(secondId);
        assertTrue(firstDeleted.getDeleted());
        assertEquals(firstId, firstDeleted.getUniqueDeleted());
        assertTrue(secondDeleted.getDeleted());
        assertEquals(secondId, secondDeleted.getUniqueDeleted());
    }

    private static TagVirtualSaveReqVO buildReq(String domainType, String name, String code, String expressionJson) {
        TagVirtualSaveReqVO reqVO = new TagVirtualSaveReqVO();
        reqVO.setDomainType(domainType);
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setExpressionJson(expressionJson);
        reqVO.setExpressionSummary(name + "规则摘要");
        reqVO.setUsageScenario(name + "场景");
        reqVO.setStatus(STATUS_ENABLED);
        return reqVO;
    }

}
