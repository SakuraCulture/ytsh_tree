package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagObjectRelationMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(TagObjectRelationServiceImpl.class)
class TagObjectRelationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TagObjectRelationService tagObjectRelationService;

    @Resource
    private TagObjectRelationMapper tagObjectRelationMapper;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Test
    void saveManualRelations_shouldCreateAndSoftDeleteManualOnly() {
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, STATUS_ENABLED, "manual_1");
        Long tagValueId2 = createTagValue(dimensionId, STATUS_ENABLED, "manual_2");
        Long tagValueId3 = createTagValue(dimensionId, STATUS_ENABLED, "manual_3");

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "1001",
                List.of(tagValueId1, tagValueId1, tagValueId2));

        TagObjectRelationDO firstTag2Relation = getRelation("1001", tagValueId2, SOURCE_TYPE_MANUAL, "");
        assertNotNull(firstTag2Relation);

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "1001",
                List.of(tagValueId2, tagValueId3));

        List<TagObjectRelationDO> relations = tagObjectRelationMapper.selectByObjectIdIgnoreDeleted(OBJECT_TYPE_SPU, "1001");
        assertEquals(3, relations.size());

        TagObjectRelationDO disabledRelation = getRelation("1001", tagValueId1, SOURCE_TYPE_MANUAL, "");
        assertEquals(RELATION_STATUS_DISABLED, disabledRelation.getStatus());
        assertNotNull(disabledRelation.getExpireTime());
        assertFalse(Boolean.TRUE.equals(disabledRelation.getDeleted()));
        assertEquals(0L, disabledRelation.getUniqueDeleted());

        TagObjectRelationDO restoredRelation = getRelation("1001", tagValueId2, SOURCE_TYPE_MANUAL, "");
        assertEquals(firstTag2Relation.getId(), restoredRelation.getId());
        assertEquals(RELATION_STATUS_ENABLED, restoredRelation.getStatus());
        assertNotNull(restoredRelation.getEffectiveTime());
        assertNull(restoredRelation.getExpireTime());
        assertEquals("", restoredRelation.getSourceRef());

        TagObjectRelationDO newRelation = getRelation("1001", tagValueId3, SOURCE_TYPE_MANUAL, "");
        assertEquals(RELATION_STATUS_ENABLED, newRelation.getStatus());
        assertNotNull(newRelation.getEffectiveTime());
        assertNull(newRelation.getExpireTime());
    }

    @Test
    void saveManualRelations_shouldNotAffectRuleRelations() {
        Long dimensionId = createL3Dimension();
        Long ruleTag = createTagValue(dimensionId, STATUS_ENABLED, "rule_keep");
        Long manualTag = createTagValue(dimensionId, STATUS_ENABLED, "manual_keep");

        tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "1101", "scene_rule", List.of(ruleTag));
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "1101", List.of(manualTag));

        List<TagObjectRelationDO> active = tagObjectRelationService.getActiveRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "1101");
        assertEquals(2, active.size());
        assertTrue(active.stream().anyMatch(item -> Objects.equals(item.getTagValueId(), ruleTag)));
        assertTrue(active.stream().anyMatch(item -> Objects.equals(item.getTagValueId(), manualTag)));
    }

    @Test
    void saveRuleRelations_shouldOnlyOverrideSameSourceRef() {
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, STATUS_ENABLED, "rule_1");
        Long tagValueId2 = createTagValue(dimensionId, STATUS_ENABLED, "rule_2");
        Long tagValueId3 = createTagValue(dimensionId, STATUS_ENABLED, "rule_3");

        tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "2001", "rule-A", List.of(tagValueId1, tagValueId2));
        tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "2001", "rule-B", List.of(tagValueId3));

        TagObjectRelationDO firstRuleATag2 = getRelation("2001", tagValueId2, SOURCE_TYPE_RULE, "rule-A");
        assertNotNull(firstRuleATag2);

        tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "2001", "rule-A", List.of(tagValueId2));

        List<TagObjectRelationDO> relations = tagObjectRelationMapper.selectByObjectIdIgnoreDeleted(OBJECT_TYPE_SPU, "2001");
        assertEquals(3, relations.size());

        TagObjectRelationDO disabledRuleATag1 = getRelation("2001", tagValueId1, SOURCE_TYPE_RULE, "rule-A");
        assertEquals(RELATION_STATUS_DISABLED, disabledRuleATag1.getStatus());
        assertNotNull(disabledRuleATag1.getExpireTime());

        TagObjectRelationDO restoredRuleATag2 = getRelation("2001", tagValueId2, SOURCE_TYPE_RULE, "rule-A");
        assertEquals(firstRuleATag2.getId(), restoredRuleATag2.getId());
        assertEquals(RELATION_STATUS_ENABLED, restoredRuleATag2.getStatus());
        assertNotNull(restoredRuleATag2.getEffectiveTime());
        assertNull(restoredRuleATag2.getExpireTime());

        TagObjectRelationDO untouchedRuleB = getRelation("2001", tagValueId3, SOURCE_TYPE_RULE, "rule-B");
        assertEquals(RELATION_STATUS_ENABLED, untouchedRuleB.getStatus());
        assertEquals("rule-B", untouchedRuleB.getSourceRef());
    }

    @Test
    void saveRuleRelations_whenSourceRefBlank_shouldThrow() {
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, STATUS_ENABLED, "rule_invalid_ref");

        assertServiceException(() -> tagObjectRelationService.saveRuleRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "3001",
                " ", List.of(tagValueId)), TAG_SOURCE_REF_REQUIRED);
    }

    @Test
    void saveManualRelations_whenTagValueIsNotProductDomain_shouldThrow() {
        Long storeDimensionId = createL3Dimension("STORE");
        Long tagValueId = createTagValue(storeDimensionId, STATUS_ENABLED, "store_only");

        assertServiceException(() -> tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "4001",
                List.of(tagValueId)), TAG_VALUE_NOT_PRODUCT_DOMAIN);
    }

    @Test
    void saveManualRelations_whenTagValueDisabled_shouldThrow() {
        Long dimensionId = createL3Dimension();
        Long disabledTagValueId = createTagValue(dimensionId, STATUS_DISABLED, "disabled_tag");

        assertServiceException(() -> tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "5001",
                List.of(disabledTagValueId)), TAG_VALUE_DISABLED);
    }

    @Test
    void saveManualRelations_whenTagValueDimensionNotL3_shouldThrow() {
        Long l1Id = createDimension(DOMAIN_TYPE_PRODUCT, ROOT_PARENT_ID, LEVEL_L1, "PRODUCT-L1", "product_l1");
        Long l2Id = createDimension(DOMAIN_TYPE_PRODUCT, l1Id, LEVEL_L2, "PRODUCT-L2", "product_l2");
        Long tagValueId = createTagValue(l2Id, STATUS_ENABLED, "non_l3_tag");

        assertServiceException(() -> tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "5002",
                List.of(tagValueId)), TAG_VALUE_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void saveManualRelations_whenTagValueDimensionDisabled_shouldThrow() {
        Long dimensionId = createL3Dimension(DOMAIN_TYPE_PRODUCT, STATUS_DISABLED);
        Long tagValueId = createTagValue(dimensionId, STATUS_ENABLED, "disabled_dimension_tag");

        assertServiceException(() -> tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "5003",
                List.of(tagValueId)), TAG_VALUE_DISABLED);
    }

    @Test
    void saveManualRelations_whenTagValueNotExists_shouldThrow() {
        assertServiceException(() -> tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "6001",
                List.of(999999L)), TAG_VALUE_NOT_EXISTS);
    }

    @Test
    void saveManualRelations_whenObjectTypeInvalid_shouldThrow() {
        assertServiceException(() -> tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, "SKU", "7001",
                List.of()), TAG_OBJECT_TYPE_INVALID);
    }

    @Test
    void getActiveRelationsByObjectIds_shouldReturnEnabledRelationsOnly() {
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, STATUS_ENABLED, "batch_1");
        Long tagValueId2 = createTagValue(dimensionId, STATUS_ENABLED, "batch_2");
        Long tagValueId3 = createTagValue(dimensionId, STATUS_ENABLED, "batch_3");

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "8001", List.of(tagValueId1, tagValueId2));
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "8001", List.of(tagValueId2));
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "8002", List.of(tagValueId3));

        List<TagObjectRelationDO> active = tagObjectRelationService.getActiveRelationsByObjectIds(
                DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, List.of("8001", "8002"));

        assertEquals(2, active.stream().map(TagObjectRelationDO::getObjectId).distinct().count());
        assertTrue(active.stream().noneMatch(item -> Objects.equals(item.getTagValueId(), tagValueId1)));
        assertEquals(List.of(tagValueId2, tagValueId3), active.stream()
                .map(TagObjectRelationDO::getTagValueId)
                .sorted()
                .collect(Collectors.toList()));
    }

    @Test
    void saveManualRelations_whenProductSpuObjectIdNotNumeric_shouldThrow() {
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, STATUS_ENABLED, "spu_non_numeric");

        assertServiceException(() -> tagObjectRelationService.saveManualRelations(
                DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, "SPU-INVALID", List.of(tagValueId)), TAG_OBJECT_TYPE_INVALID);
    }

    @Test
    void saveManualRelations_shouldSupportStoreProductStringObjectId() {
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, STATUS_ENABLED, "store_product_manual_1");
        Long tagValueId2 = createTagValue(dimensionId, STATUS_ENABLED, "store_product_manual_2");

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "STORE-PRODUCT-001",
                List.of(tagValueId1, tagValueId2));

        List<TagObjectRelationDO> relations = tagObjectRelationService.getActiveRelations(
                DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "STORE-PRODUCT-001");

        assertEquals(2, relations.size());
        assertEquals(List.of(tagValueId1, tagValueId2), relations.stream()
                .map(TagObjectRelationDO::getTagValueId)
                .sorted()
                .collect(Collectors.toList()));
        assertTrue(relations.stream().allMatch(item -> Objects.equals(item.getObjectId(), "STORE-PRODUCT-001")));
        assertTrue(relations.stream().allMatch(item -> Objects.equals(item.getSourceType(), SOURCE_TYPE_MANUAL)));
    }

    @Test
    void saveManualRelations_shouldDisableRemovedRelationForStoreProductStringObjectId() {
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, STATUS_ENABLED, "store_product_override_1");
        Long tagValueId2 = createTagValue(dimensionId, STATUS_ENABLED, "store_product_override_2");

        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "STORE-PRODUCT-002",
                List.of(tagValueId1, tagValueId2));
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "STORE-PRODUCT-002",
                List.of(tagValueId2));

        List<TagObjectRelationDO> relations = tagObjectRelationMapper.selectByObjectIdIgnoreDeleted(
                OBJECT_TYPE_STORE_PRODUCT, "STORE-PRODUCT-002");
        assertEquals(2, relations.size());

        TagObjectRelationDO disabledRelation = getRelation("STORE-PRODUCT-002", tagValueId1, SOURCE_TYPE_MANUAL, "");
        assertEquals(RELATION_STATUS_DISABLED, disabledRelation.getStatus());
        assertNotNull(disabledRelation.getExpireTime());

        TagObjectRelationDO keptRelation = getRelation("STORE-PRODUCT-002", tagValueId2, SOURCE_TYPE_MANUAL, "");
        assertEquals(RELATION_STATUS_ENABLED, keptRelation.getStatus());
        assertNull(keptRelation.getExpireTime());
    }

    @Test
    void saveManualRelations_whenStoreProductObjectIdBlank_shouldThrow() {
        assertServiceException(() -> tagObjectRelationService.saveManualRelations(
                DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "   ", List.of()), TAG_OBJECT_TYPE_INVALID);
    }

    private TagObjectRelationDO getRelation(String objectId, Long tagValueId, String sourceType, String sourceRef) {
        return tagObjectRelationMapper.selectByObjectIdIgnoreDeleted(OBJECT_TYPE_SPU, objectId).stream()
                .filter(item -> Objects.equals(item.getTagValueId(), tagValueId))
                .filter(item -> Objects.equals(item.getSourceType(), sourceType))
                .filter(item -> Objects.equals(item.getSourceRef(), sourceRef))
                .findFirst()
                .orElseGet(() -> tagObjectRelationMapper.selectByObjectIdIgnoreDeleted(OBJECT_TYPE_STORE_PRODUCT, objectId).stream()
                        .filter(item -> Objects.equals(item.getTagValueId(), tagValueId))
                        .filter(item -> Objects.equals(item.getSourceType(), sourceType))
                        .filter(item -> Objects.equals(item.getSourceRef(), sourceRef))
                        .findFirst()
                        .orElse(null));
    }

    private Long createL3Dimension() {
        return createL3Dimension(DOMAIN_TYPE_PRODUCT);
    }

    private Long createL3Dimension(String domainType) {
        return createL3Dimension(domainType, STATUS_ENABLED);
    }

    private Long createL3Dimension(String domainType, Integer status) {
        Long l1Id = createDimension(domainType, ROOT_PARENT_ID, LEVEL_L1, domainType + "-L1", domainType.toLowerCase() + "_l1", STATUS_ENABLED);
        Long l2Id = createDimension(domainType, l1Id, LEVEL_L2, domainType + "-L2", domainType.toLowerCase() + "_l2", STATUS_ENABLED);
        return createDimension(domainType, l2Id, LEVEL_L3, domainType + "-L3", domainType.toLowerCase() + "_l3", status);
    }

    private Long createDimension(String domainType, Long parentId, Integer level, String name, String code) {
        return createDimension(domainType, parentId, level, name, code, STATUS_ENABLED);
    }

    private Long createDimension(String domainType, Long parentId, Integer level, String name, String code, Integer status) {
        TagDimensionDO dimension = TagDimensionDO.builder()
                .domainType(domainType)
                .parentId(parentId)
                .level(level)
                .name(name)
                .code(code)
                .sort(10 * level)
                .status(status)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagDimensionMapper.insert(dimension);
        return dimension.getId();
    }

    private Long createTagValue(Long dimensionId, Integer status, String code) {
        TagValueDO tagValue = TagValueDO.builder()
                .dimensionId(dimensionId)
                .name(code)
                .code(code)
                .tagMethod(TAG_METHOD_MANUAL)
                .dataSource("test")
                .updateFrequency("daily")
                .logicDescription(code)
                .sort(10)
                .status(status)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagValueMapper.insert(tagValue);
        return tagValue.getId();
    }
}
