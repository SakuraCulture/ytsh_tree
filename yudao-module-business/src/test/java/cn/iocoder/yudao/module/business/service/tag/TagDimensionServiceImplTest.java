package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(TagDimensionServiceImpl.class)
class TagDimensionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TagDimensionService tagDimensionService;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Test
    void createTagDimension_shouldCreateThreeLevelTree() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        Long l2Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));
        Long l3Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", l2Id, LEVEL_L3, "价格带", "price_band", 30));

        TagDimensionDO l3 = tagDimensionMapper.selectById(l3Id);
        assertEquals("PRODUCT", l3.getDomainType());
        assertEquals(l2Id, l3.getParentId());
        assertEquals(LEVEL_L3, l3.getLevel());
        assertEquals("价格带", l3.getName());
        assertEquals("price_band", l3.getCode());
        assertEquals(30, l3.getSort());
        assertEquals(STATUS_ENABLED, l3.getStatus());
        assertEquals(0L, l3.getUniqueDeleted());

        List<TagDimensionDO> list = tagDimensionService.getTagDimensionList("PRODUCT", l2Id, LEVEL_L3);
        assertEquals(1, list.size());
        assertEquals(l3Id, list.get(0).getId());
    }

    @Test
    void createTagDimension_whenL3ParentIsL1_shouldThrowLevelError() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));

        assertServiceException(() -> tagDimensionService.createTagDimension(
                buildReq("PRODUCT", l1Id, LEVEL_L3, "价格带", "price_band", 30)),
                TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void createTagDimension_whenSameParentCodeDuplicate_shouldThrowCodeExists() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));

        assertServiceException(() -> tagDimensionService.createTagDimension(
                buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性2", "price", 30)),
                TAG_DIMENSION_CODE_EXISTS);
    }

    @Test
    void createTagDimension_whenParentDomainTypeDifferent_shouldThrowLevelError() {
        Long productL1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));

        assertServiceException(() -> tagDimensionService.createTagDimension(
                buildReq("STORE", productL1Id, LEVEL_L2, "门店属性", "store", 20)),
                TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void createTagDimension_whenStatusInvalid_shouldThrowLevelError() {
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10);
        reqVO.setStatus(2);

        assertServiceException(() -> tagDimensionService.createTagDimension(reqVO), TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void createTagDimension_whenStatusNull_shouldDefaultEnabled() {
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10);
        reqVO.setStatus(null);

        Long id = tagDimensionService.createTagDimension(reqVO);

        assertEquals(STATUS_ENABLED, tagDimensionMapper.selectById(id).getStatus());
    }

    @Test
    void updateTagDimension_whenParentDomainTypeDifferent_shouldThrowLevelError() {
        Long productL1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        Long storeL1Id = tagDimensionService.createTagDimension(buildReq("STORE", ROOT_PARENT_ID, LEVEL_L1, "门店属性", "store_base", 10));
        Long storeL2Id = tagDimensionService.createTagDimension(buildReq("STORE", storeL1Id, LEVEL_L2, "门店价格", "store_price", 20));
        TagDimensionSaveReqVO reqVO = buildReq("STORE", productL1Id, LEVEL_L2, "门店价格", "store_price", 20);
        reqVO.setId(storeL2Id);

        assertServiceException(() -> tagDimensionService.updateTagDimension(reqVO), TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void updateTagDimension_whenHasChildrenAndChangeLevel_shouldThrowHasChildren() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        Long l2Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));
        tagDimensionService.createTagDimension(buildReq("PRODUCT", l2Id, LEVEL_L3, "价格带", "price_band", 30));
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "价格属性", "price", 20);
        reqVO.setId(l2Id);

        assertServiceException(() -> tagDimensionService.updateTagDimension(reqVO), TAG_DIMENSION_HAS_CHILDREN);
    }

    @Test
    void updateTagDimension_whenHasChildrenAndChangeParent_shouldThrowHasChildren() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        Long otherL1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "其他属性", "other", 20));
        Long l2Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));
        tagDimensionService.createTagDimension(buildReq("PRODUCT", l2Id, LEVEL_L3, "价格带", "price_band", 30));
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", otherL1Id, LEVEL_L2, "价格属性", "price", 20);
        reqVO.setId(l2Id);

        assertServiceException(() -> tagDimensionService.updateTagDimension(reqVO), TAG_DIMENSION_HAS_CHILDREN);
    }

    @Test
    void updateTagDimension_whenHasChildrenAndChangeDomainType_shouldThrowHasChildren() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));
        TagDimensionSaveReqVO reqVO = buildReq("STORE", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10);
        reqVO.setId(l1Id);

        assertServiceException(() -> tagDimensionService.updateTagDimension(reqVO), TAG_DIMENSION_HAS_CHILDREN);
    }

    @Test
    void updateTagDimension_whenHasChildrenAndChangeNonStructureFields_shouldUpdate() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性-新", "base_new", 99);
        reqVO.setId(l1Id);
        reqVO.setStatus(STATUS_DISABLED);
        reqVO.setDescription("新说明");

        tagDimensionService.updateTagDimension(reqVO);

        TagDimensionDO tagDimension = tagDimensionMapper.selectById(l1Id);
        assertEquals("基础属性-新", tagDimension.getName());
        assertEquals("base_new", tagDimension.getCode());
        assertEquals(99, tagDimension.getSort());
        assertEquals(STATUS_DISABLED, tagDimension.getStatus());
        assertEquals("新说明", tagDimension.getDescription());
    }

    @Test
    void updateTagDimension_whenNoChildrenAndChangeStructureFields_shouldUpdate() {
        Long productL1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        Long storeL1Id = tagDimensionService.createTagDimension(buildReq("STORE", ROOT_PARENT_ID, LEVEL_L1, "门店属性", "store_base", 10));
        Long l2Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", productL1Id, LEVEL_L2, "价格属性", "price", 20));
        TagDimensionSaveReqVO reqVO = buildReq("STORE", storeL1Id, LEVEL_L2, "门店价格", "store_price", 30);
        reqVO.setId(l2Id);

        tagDimensionService.updateTagDimension(reqVO);

        TagDimensionDO tagDimension = tagDimensionMapper.selectById(l2Id);
        assertEquals("STORE", tagDimension.getDomainType());
        assertEquals(storeL1Id, tagDimension.getParentId());
        assertEquals(LEVEL_L2, tagDimension.getLevel());
    }

    @Test
    void updateTagDimension_whenStatusInvalid_shouldThrowLevelError() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10);
        reqVO.setId(l1Id);
        reqVO.setStatus(2);

        assertServiceException(() -> tagDimensionService.updateTagDimension(reqVO), TAG_DIMENSION_LEVEL_ERROR);
    }

    @Test
    void updateTagDimension_whenStatusNull_shouldDefaultEnabled() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        TagDimensionSaveReqVO reqVO = buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10);
        reqVO.setId(l1Id);
        reqVO.setStatus(null);

        tagDimensionService.updateTagDimension(reqVO);

        assertEquals(STATUS_ENABLED, tagDimensionMapper.selectById(l1Id).getStatus());
    }

    @Test
    void deleteTagDimension_whenHasChildren_shouldThrowHasChildren() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));

        assertServiceException(() -> tagDimensionService.deleteTagDimension(l1Id), TAG_DIMENSION_HAS_CHILDREN);
    }

    @Test
    void deleteTagDimension_shouldReleaseUniqueCodeByUniqueDeleted() {
        Long l1Id = tagDimensionService.createTagDimension(buildReq("PRODUCT", ROOT_PARENT_ID, LEVEL_L1, "基础属性", "base", 10));
        Long firstId = tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性", "price", 20));

        tagDimensionService.deleteTagDimension(firstId);
        Long secondId = tagDimensionService.createTagDimension(buildReq("PRODUCT", l1Id, LEVEL_L2, "价格属性2", "price", 30));
        tagDimensionService.deleteTagDimension(secondId);

        TagDimensionDO firstDeleted = tagDimensionMapper.selectByIdIgnoreDeleted(firstId);
        TagDimensionDO secondDeleted = tagDimensionMapper.selectByIdIgnoreDeleted(secondId);
        assertTrue(firstDeleted.getDeleted());
        assertEquals(firstId, firstDeleted.getUniqueDeleted());
        assertTrue(secondDeleted.getDeleted());
        assertEquals(secondId, secondDeleted.getUniqueDeleted());
    }

    private static TagDimensionSaveReqVO buildReq(String domainType, Long parentId, Integer level,
                                                  String name, String code, Integer sort) {
        TagDimensionSaveReqVO reqVO = new TagDimensionSaveReqVO();
        reqVO.setDomainType(domainType);
        reqVO.setParentId(parentId);
        reqVO.setLevel(level);
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setSort(sort);
        reqVO.setStatus(STATUS_ENABLED);
        reqVO.setDescription(name + "说明");
        return reqVO;
    }

}
