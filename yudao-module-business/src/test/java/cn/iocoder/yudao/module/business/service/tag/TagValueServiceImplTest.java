package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagSelectableValueRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.DOMAIN_TYPE_PRODUCT;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L1;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L2;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L3;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.OBJECT_TYPE_STORE_PRODUCT;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.ROOT_PARENT_ID;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.STATUS_ENABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.TAG_METHOD_MANUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TagValueServiceImpl.class)
class TagValueServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TagValueService tagValueService;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Test
    void getSelectableTagValuesForObject_shouldReturnProductValuesForStoreProduct() {
        Long l1Id = createDimension(ROOT_PARENT_ID, LEVEL_L1, "经营属性", "operation");
        Long l2Id = createDimension(l1Id, LEVEL_L2, "商品角色", "product_role");
        Long l3Id = createDimension(l2Id, LEVEL_L3, "功能角色", "function_role");
        Long tagValueId = createTagValue(l3Id, "store_product_tag");

        List<TagSelectableValueRespVO> result = tagValueService.getSelectableTagValuesForObject(OBJECT_TYPE_STORE_PRODUCT);

        assertEquals(1, result.size());
        assertEquals(tagValueId, result.get(0).getTagValueId());
        assertEquals("store_product_tag", result.get(0).getTagValueCode());
        assertEquals("store_product_tag", result.get(0).getTagValueName());
        assertEquals(l3Id, result.get(0).getDimensionId());
        assertEquals("功能角色", result.get(0).getDimensionName());
        assertEquals("经营属性 / 商品角色 / 功能角色", result.get(0).getDimensionPath());
        assertEquals(STATUS_ENABLED, result.get(0).getStatus());
    }

    private Long createDimension(Long parentId, Integer level, String name, String code) {
        TagDimensionDO dimension = TagDimensionDO.builder()
                .domainType(DOMAIN_TYPE_PRODUCT)
                .parentId(parentId)
                .level(level)
                .name(name)
                .code(code)
                .sort(10 * level)
                .status(STATUS_ENABLED)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagDimensionMapper.insert(dimension);
        return dimension.getId();
    }

    private Long createTagValue(Long dimensionId, String code) {
        TagValueDO tagValue = TagValueDO.builder()
                .dimensionId(dimensionId)
                .name(code)
                .code(code)
                .tagMethod(TAG_METHOD_MANUAL)
                .dataSource("test")
                .updateFrequency("daily")
                .logicDescription(code)
                .sort(10)
                .status(STATUS_ENABLED)
                .uniqueDeleted(0L)
                .tenantId(1L)
                .build();
        tagValueMapper.insert(tagValue);
        return tagValue.getId();
    }
}
