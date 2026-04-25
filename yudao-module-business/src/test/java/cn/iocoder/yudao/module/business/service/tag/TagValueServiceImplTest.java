package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({TagDimensionServiceImpl.class, TagValueServiceImpl.class})
class TagValueServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TagDimensionService tagDimensionService;

    @Resource
    private TagValueService tagValueService;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Test
    void createTagValue_whenDimensionIsL3_shouldCreate() {
        Long l3Id = createL3Dimension();

        Long id = tagValueService.createTagValue(buildSaveReq(l3Id, "高价值用户", "high_value"));

        TagValueDO value = tagValueMapper.selectById(id);
        assertEquals(l3Id, value.getDimensionId());
        assertEquals("高价值用户", value.getName());
        assertEquals("high_value", value.getCode());
        assertEquals("MANUAL", value.getTagMethod());
        assertEquals(STATUS_ENABLED, value.getStatus());
        assertEquals(0L, value.getUniqueDeleted());
    }

    @Test
    void createTagValue_whenStatusInvalid_shouldThrowError() {
        Long l3Id = createL3Dimension();
        TagValueSaveReqVO reqVO = buildSaveReq(l3Id, "高价值用户", "high_value");
        reqVO.setStatus(99);

        assertServiceException(() -> tagValueService.createTagValue(reqVO), TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void createTagValue_whenStatusNull_shouldDefaultEnabled() {
        Long l3Id = createL3Dimension();
        TagValueSaveReqVO reqVO = buildSaveReq(l3Id, "高价值用户", "high_value");
        reqVO.setStatus(null);

        Long id = tagValueService.createTagValue(reqVO);

        assertEquals(STATUS_ENABLED, tagValueMapper.selectById(id).getStatus());
    }

    @Test
    void createTagValue_whenDimensionIsL2_shouldThrowDimensionLevelError() {
        Long l1Id = createDimension("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "商品", "product");
        Long l2Id = createDimension("PRODUCT", l1Id, LEVEL_L2, "品牌", "brand");

        assertServiceException(() -> tagValueService.createTagValue(buildSaveReq(l2Id, "品牌A", "brand_a")),
                TAG_VALUE_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void createTagValue_whenCodeDuplicateInSameDimension_shouldThrowCodeExists() {
        Long l3Id = createL3Dimension();
        tagValueService.createTagValue(buildSaveReq(l3Id, "高价值用户", "high_value"));

        assertServiceException(() -> tagValueService.createTagValue(buildSaveReq(l3Id, "高价值用户2", "high_value")),
                TAG_VALUE_CODE_EXISTS);
    }

    @Test
    void updateTagValue_whenStatusInvalid_shouldThrowError() {
        Long l3Id = createL3Dimension();
        Long id = tagValueService.createTagValue(buildSaveReq(l3Id, "高价值用户", "high_value"));
        TagValueSaveReqVO reqVO = buildSaveReq(l3Id, "高价值用户-更新", "high_value_update");
        reqVO.setId(id);
        reqVO.setStatus(99);

        assertServiceException(() -> tagValueService.updateTagValue(reqVO), TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void updateTagValue_shouldUpdateSuccessfully() {
        Long l3Id = createL3Dimension();
        Long id = tagValueService.createTagValue(buildSaveReq(l3Id, "高价值用户", "high_value"));
        TagValueSaveReqVO reqVO = buildSaveReq(l3Id, "高价值用户-更新", "high_value_update");
        reqVO.setId(id);
        reqVO.setStatus(null);
        reqVO.setSort(20);

        tagValueService.updateTagValue(reqVO);

        TagValueDO value = tagValueMapper.selectById(id);
        assertEquals("高价值用户-更新", value.getName());
        assertEquals("high_value_update", value.getCode());
        assertEquals(STATUS_ENABLED, value.getStatus());
        assertEquals(20, value.getSort());
    }

    @Test
    void deleteTagValue_shouldReleaseUniqueDeletedBeforeLogicalDelete() {
        Long l3Id = createL3Dimension();
        Long id = tagValueService.createTagValue(buildSaveReq(l3Id, "高价值用户", "high_value"));

        tagValueService.deleteTagValue(id);

        TagValueDO deleted = tagValueMapper.selectByIdIgnoreDeleted(id);
        assertNotNull(deleted);
        assertEquals(id, deleted.getUniqueDeleted());
        assertNotNull(deleted.getDeleted());
    }

    @Test
    void importTagValueList_shouldCreateHierarchyAndBeIdempotent() {
        TagValueImportReqVO first = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "高价格", "high_price");
        TagValueImportReqVO second = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "低价格", "low_price");

        TagValueImportRespVO firstResp = tagValueService.importTagValueList(List.of(first, second), true);
        TagValueImportRespVO secondResp = tagValueService.importTagValueList(List.of(first, second), true);

        assertEquals(List.of("高价格", "低价格"), firstResp.getCreateNames());
        assertTrue(firstResp.getUpdateNames().isEmpty());
        assertTrue(firstResp.getFailureNames().isEmpty());

        assertTrue(secondResp.getCreateNames().isEmpty());
        assertEquals(List.of("高价格", "低价格"), secondResp.getUpdateNames());
        assertTrue(secondResp.getFailureNames().isEmpty());

        List<TagDimensionDO> l1List = tagDimensionMapper.selectList("PRODUCT", ROOT_PARENT_ID, LEVEL_L1);
        assertEquals(1, l1List.size());
        List<TagDimensionDO> l2List = tagDimensionMapper.selectList("PRODUCT", l1List.get(0).getId(), LEVEL_L2);
        assertEquals(1, l2List.size());
        List<TagDimensionDO> l3List = tagDimensionMapper.selectList("PRODUCT", l2List.get(0).getId(), LEVEL_L3);
        assertEquals(1, l3List.size());
        assertEquals(2, tagValueMapper.selectListByDimensionId(l3List.get(0).getId()).size());
    }

    @Test
    void importTagValueList_whenEmpty_shouldThrowListEmpty() {
        assertServiceException(() -> tagValueService.importTagValueList(List.of(), true), TAG_VALUE_IMPORT_LIST_IS_EMPTY);
    }

    @Test
    void importTagValueList_whenPartialFailure_shouldCollectFailureAndContinue() {
        TagValueImportReqVO success = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "高价格", "high_price");
        TagValueImportReqVO failure = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "规则异常", "rule_error");
        failure.setTagMethod("INVALID_METHOD");

        TagValueImportRespVO respVO = tagValueService.importTagValueList(List.of(success, failure), true);

        assertEquals(List.of("高价格"), respVO.getCreateNames());
        assertTrue(respVO.getUpdateNames().isEmpty());
        assertEquals(1, respVO.getFailureNames().size());
        assertTrue(respVO.getFailureNames().keySet().stream().anyMatch(key -> key.contains("规则异常")));
        assertEquals(1, tagValueMapper.selectListByDimensionId(createL3DimensionIdIfExists()).size());
    }

    @Test
    void importTagValueList_whenStatusInvalid_shouldCaptureRowFailure() {
        TagValueImportReqVO invalid = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "状态异常", "status_error");
        invalid.setStatus(99);
        TagValueImportReqVO success = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "正常记录", "normal_value");

        TagValueImportRespVO respVO = tagValueService.importTagValueList(List.of(invalid, success), true);

        assertEquals(List.of("正常记录"), respVO.getCreateNames());
        assertTrue(respVO.getUpdateNames().isEmpty());
        assertEquals(1, respVO.getFailureNames().size());
        assertTrue(respVO.getFailureNames().keySet().stream().anyMatch(key -> key.contains("状态异常")));
        assertEquals(1, tagValueMapper.selectListByDimensionId(createL3DimensionIdIfExists()).size());
    }

    @Test
    void importTagValueList_whenRequiredFieldBlank_shouldCaptureRowFailure() {
        TagValueImportReqVO blankField = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "", "blank_name");
        TagValueImportReqVO success = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "正常记录", "normal_value");

        TagValueImportRespVO respVO = tagValueService.importTagValueList(List.of(blankField, success), true);

        assertEquals(List.of("正常记录"), respVO.getCreateNames());
        assertTrue(respVO.getUpdateNames().isEmpty());
        assertEquals(1, respVO.getFailureNames().size());
        assertTrue(respVO.getFailureNames().keySet().stream().anyMatch(key -> key.contains("<blank>")));
        assertEquals(1, tagValueMapper.selectListByDimensionId(createL3DimensionIdIfExists()).size());
    }

    @Test
    void importTagValueList_whenDuplicateFailureNames_shouldPreserveBothEntries() {
        TagValueImportReqVO first = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "重复名称", "duplicate_name_1");
        first.setStatus(99);
        TagValueImportReqVO second = buildImportReq("PRODUCT", "商品属性", "attr", "价格属性", "price", "价格带", "price_band",
                "重复名称", "duplicate_name_2");
        second.setTagMethod("INVALID_METHOD");

        TagValueImportRespVO respVO = tagValueService.importTagValueList(List.of(first, second), true);

        assertTrue(respVO.getCreateNames().isEmpty());
        assertTrue(respVO.getUpdateNames().isEmpty());
        assertEquals(2, respVO.getFailureNames().size());
        assertTrue(respVO.getFailureNames().keySet().stream().allMatch(key -> key.contains("重复名称")));
        assertNotEquals(respVO.getFailureNames().keySet().stream().findFirst().orElseThrow(),
                respVO.getFailureNames().keySet().stream().skip(1).findFirst().orElseThrow());
    }

    private Long createL3Dimension() {
        Long l1Id = createDimension("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "商品属性", "attr");
        Long l2Id = createDimension("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price");
        return createDimension("PRODUCT", l2Id, LEVEL_L3, "价格带", "price_band");
    }

    private Long createL3DimensionIdIfExists() {
        List<TagDimensionDO> l1List = tagDimensionMapper.selectList("PRODUCT", ROOT_PARENT_ID, LEVEL_L1);
        assertFalse(l1List.isEmpty());
        List<TagDimensionDO> l2List = tagDimensionMapper.selectList("PRODUCT", l1List.get(0).getId(), LEVEL_L2);
        assertFalse(l2List.isEmpty());
        List<TagDimensionDO> l3List = tagDimensionMapper.selectList("PRODUCT", l2List.get(0).getId(), LEVEL_L3);
        assertEquals(1, l3List.size());
        return l3List.get(0).getId();
    }

    private Long createDimension(String domainType, Long parentId, Integer level, String name, String code) {
        cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionSaveReqVO reqVO =
                new cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionSaveReqVO();
        reqVO.setDomainType(domainType);
        reqVO.setParentId(parentId);
        reqVO.setLevel(level);
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setSort(10);
        reqVO.setStatus(STATUS_ENABLED);
        reqVO.setDescription(name + "说明");
        return tagDimensionService.createTagDimension(reqVO);
    }

    private static TagValueSaveReqVO buildSaveReq(Long dimensionId, String name, String code) {
        TagValueSaveReqVO reqVO = new TagValueSaveReqVO();
        reqVO.setDimensionId(dimensionId);
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setTagMethod("MANUAL");
        reqVO.setDataSource("运营后台");
        reqVO.setUpdateFrequency("每日");
        reqVO.setLogicDescription(name + "逻辑");
        reqVO.setSort(10);
        reqVO.setStatus(STATUS_ENABLED);
        return reqVO;
    }

    private static TagValueImportReqVO buildImportReq(String domainType,
                                                      String l1Name, String l1Code,
                                                      String l2Name, String l2Code,
                                                      String l3Name, String l3Code,
                                                      String tagValueName, String tagValueCode) {
        TagValueImportReqVO reqVO = new TagValueImportReqVO();
        reqVO.setDomainType(domainType);
        reqVO.setL1Name(l1Name);
        reqVO.setL1Code(l1Code);
        reqVO.setL2Name(l2Name);
        reqVO.setL2Code(l2Code);
        reqVO.setL3Name(l3Name);
        reqVO.setL3Code(l3Code);
        reqVO.setTagValueName(tagValueName);
        reqVO.setTagValueCode(tagValueCode);
        reqVO.setTagMethod("MANUAL");
        reqVO.setDataSource("运营后台");
        reqVO.setUpdateFrequency("每日");
        reqVO.setLogicDescription(tagValueName + "逻辑");
        reqVO.setSort(10);
        reqVO.setStatus(STATUS_ENABLED);
        return reqVO;
    }

}
