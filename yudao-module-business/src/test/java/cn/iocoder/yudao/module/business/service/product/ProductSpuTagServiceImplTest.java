package cn.iocoder.yudao.module.business.service.product;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SpuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagObjectRelationMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import cn.iocoder.yudao.module.business.service.tag.TagObjectRelationService;
import cn.iocoder.yudao.module.business.service.tag.TagObjectRelationServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.SPU_TABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.TAG_VALUE_NOT_PRODUCT_DOMAIN;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({ProductSpuTagServiceImpl.class, TagObjectRelationServiceImpl.class})
class ProductSpuTagServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ProductSpuTagService productSpuTagService;
    @Resource
    private TagObjectRelationService tagObjectRelationService;
    @Resource
    private SpuTableMapper spuTableMapper;
    @Resource
    private TagDimensionMapper tagDimensionMapper;
    @Resource
    private TagValueMapper tagValueMapper;
    @Resource
    private TagObjectRelationMapper tagObjectRelationMapper;

    @Test
    void saveManualTags_shouldPersistOnlyExistingProductL3Tags() {
        Long spuId = createSpu(1001L);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "人群属性", "高价值带");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "high_value", "高价值商品");

        ProductSpuTagSaveReqVO reqVO = new ProductSpuTagSaveReqVO();
        reqVO.setProductSpuId(spuId);
        reqVO.setTagValueIds(List.of(tagValueId, tagValueId));

        productSpuTagService.saveManualTags(reqVO);

        List<TagObjectRelationDO> relations = tagObjectRelationMapper.selectActiveList(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId);
        assertEquals(1, relations.size());
        assertEquals(tagValueId, relations.get(0).getTagValueId());
        assertEquals(SOURCE_TYPE_MANUAL, relations.get(0).getSourceType());
    }

    @Test
    void saveManualTags_whenSpuMissing_shouldThrow() {
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "人群属性", "高价值带");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "high_value", "高价值商品");
        ProductSpuTagSaveReqVO reqVO = new ProductSpuTagSaveReqVO();
        reqVO.setProductSpuId(999999L);
        reqVO.setTagValueIds(List.of(tagValueId));

        assertServiceException(() -> productSpuTagService.saveManualTags(reqVO), SPU_TABLE_NOT_EXISTS);
    }

    @Test
    void saveManualTags_whenTagNotProductDomain_shouldThrow() {
        Long spuId = createSpu(1002L);
        Long storeL3Id = createL3Dimension("STORE", "门店属性", "门店角色", "门店能力");
        Long storeTagValueId = createTagValue(storeL3Id, STATUS_ENABLED, "store_tag", "门店标签");
        ProductSpuTagSaveReqVO reqVO = new ProductSpuTagSaveReqVO();
        reqVO.setProductSpuId(spuId);
        reqVO.setTagValueIds(List.of(storeTagValueId));

        assertServiceException(() -> productSpuTagService.saveManualTags(reqVO), TAG_VALUE_NOT_PRODUCT_DOMAIN);
    }

    @Test
    void getTagList_shouldAggregateManualAndRuleSources() {
        Long spuId = createSpu(1003L);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "价格属性", "价格带");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "mid_price", "中价格带");

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId, List.of(tagValueId));
        tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId, "rule-price", List.of(tagValueId));

        List<ProductSpuTagRespVO> result = productSpuTagService.getTagList(spuId);

        assertEquals(1, result.size());
        ProductSpuTagRespVO tag = result.get(0);
        assertEquals(tagValueId, tag.getTagValueId());
        assertEquals("mid_price", tag.getTagValueCode());
        assertEquals("中价格带", tag.getTagValueName());
        assertEquals("商品属性 / 价格属性 / 价格带", tag.getDimensionPath());
        assertEquals(List.of(SOURCE_TYPE_MANUAL, SOURCE_TYPE_RULE), tag.getSources());
        assertEquals(2, tag.getSourceDetails().size());
        assertTrue(tag.getSourceDetails().stream().anyMatch(item -> Objects.equals(item.getSourceType(), SOURCE_TYPE_MANUAL)
                && Objects.equals(item.getSourceRef(), "")
                && Objects.equals(item.getStatus(), RELATION_STATUS_ENABLED)));
        assertTrue(tag.getSourceDetails().stream().anyMatch(item -> Objects.equals(item.getSourceType(), SOURCE_TYPE_RULE)
                && Objects.equals(item.getSourceRef(), "rule-price")
                && Objects.equals(item.getStatus(), RELATION_STATUS_ENABLED)));
    }

    @Test
    void getSimpleTagList_shouldReturnGroupedTagsBySpu() {
        Long spuId1 = createSpu(1004L);
        Long spuId2 = createSpu(1005L);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "功能属性", "功能角色");
        Long tagValueId1 = createTagValue(l3Id, STATUS_ENABLED, "role_a", "角色A");
        Long tagValueId2 = createTagValue(l3Id, STATUS_ENABLED, "role_b", "角色B");

        tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId1, "rule-role", List.of(tagValueId2));
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId1, List.of(tagValueId1));
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId2, List.of(tagValueId2));

        List<ProductSpuTagSimpleRespVO> result = productSpuTagService.getSimpleTagList(List.of(spuId1, spuId2));

        assertEquals(2, result.size());
        ProductSpuTagSimpleRespVO first = result.stream().filter(item -> Objects.equals(item.getProductSpuId(), spuId1)).findFirst().orElseThrow();
        ProductSpuTagSimpleRespVO second = result.stream().filter(item -> Objects.equals(item.getProductSpuId(), spuId2)).findFirst().orElseThrow();
        assertEquals(2, first.getTags().size());
        assertEquals(List.of(tagValueId1, tagValueId2), first.getTags().stream().map(ProductSpuTagRespVO::getTagValueId).toList());
        assertEquals(1, second.getTags().size());
        assertEquals(tagValueId2, second.getTags().get(0).getTagValueId());
    }

    @Test
    void getSimpleTagList_whenSomeSpuHasNoTags_shouldReturnEmptyTagList() {
        Long spuId1 = createSpu(1006L);
        Long spuId2 = createSpu(1007L);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "场景属性", "露营场景");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "camping", "露营");

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, spuId1, List.of(tagValueId));

        List<ProductSpuTagSimpleRespVO> result = productSpuTagService.getSimpleTagList(List.of(spuId1, spuId2));

        assertEquals(2, result.size());
        ProductSpuTagSimpleRespVO tagged = result.stream().filter(item -> Objects.equals(item.getProductSpuId(), spuId1)).findFirst().orElseThrow();
        ProductSpuTagSimpleRespVO untagged = result.stream().filter(item -> Objects.equals(item.getProductSpuId(), spuId2)).findFirst().orElseThrow();
        assertEquals(1, tagged.getTags().size());
        assertTrue(untagged.getTags().isEmpty());
    }

    private Long createSpu(Long spuId) {
        spuTableMapper.insert(SpuTableDO.builder()
                .productSpuId(spuId)
                .productSpuCode("SPU-" + spuId)
                .productSpuName("SPU" + spuId)
                .productBrand("品牌A")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("厂商A")
                .productSpecTemplate("标准规格")
                .productSpuStatus(1)
                .build());
        return spuId;
    }

    private Long createL3Dimension(String domainType, String l1Name, String l2Name, String l3Name) {
        TagDimensionDO l1 = TagDimensionDO.builder()
                .domainType(domainType)
                .parentId(ROOT_PARENT_ID)
                .level(LEVEL_L1)
                .name(l1Name)
                .code((domainType + "_l1_" + l1Name).replaceAll("\\s+", "_").toLowerCase())
                .sort(10)
                .status(STATUS_ENABLED)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagDimensionMapper.insert(l1);

        TagDimensionDO l2 = TagDimensionDO.builder()
                .domainType(domainType)
                .parentId(l1.getId())
                .level(LEVEL_L2)
                .name(l2Name)
                .code((domainType + "_l2_" + l2Name).replaceAll("\\s+", "_").toLowerCase())
                .sort(20)
                .status(STATUS_ENABLED)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagDimensionMapper.insert(l2);

        TagDimensionDO l3 = TagDimensionDO.builder()
                .domainType(domainType)
                .parentId(l2.getId())
                .level(LEVEL_L3)
                .name(l3Name)
                .code((domainType + "_l3_" + l3Name).replaceAll("\\s+", "_").toLowerCase())
                .sort(30)
                .status(STATUS_ENABLED)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagDimensionMapper.insert(l3);
        return l3.getId();
    }

    private Long createTagValue(Long dimensionId, Integer status, String code, String name) {
        TagValueDO tagValue = TagValueDO.builder()
                .dimensionId(dimensionId)
                .name(name)
                .code(code)
                .tagMethod(TAG_METHOD_MANUAL)
                .dataSource("test")
                .updateFrequency("daily")
                .logicDescription(name + "逻辑")
                .sort(10)
                .status(status)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagValueMapper.insert(tagValue);
        return tagValue.getId();
    }
}
