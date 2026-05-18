package cn.iocoder.yudao.module.business.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.SpuTablePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SpuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagObjectRelationMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import cn.iocoder.yudao.module.business.service.tag.TagObjectRelationServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.DOMAIN_TYPE_PRODUCT;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L1;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L2;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L3;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.OBJECT_TYPE_SPU;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.RELATION_STATUS_ENABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.ROOT_PARENT_ID;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.SOURCE_TYPE_MANUAL;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.STATUS_ENABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.TAG_METHOD_MANUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import({SpuTableServiceImpl.class, ProductSpuTagServiceImpl.class, TagObjectRelationServiceImpl.class})
class SpuTableServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SpuTableService spuTableService;

    @Resource
    private SpuTableMapper spuTableMapper;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Resource
    private TagObjectRelationMapper tagObjectRelationMapper;

    @Test
    void getSpuTablePage_whenFilterByTag_shouldConvertStringObjectIdBackToLong() {
        insertSpu(1001L, "SPU-1001");
        insertSpu(1002L, "SPU-1002");
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "tag_filter");
        insertSpuRelation("1001", tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setTagValueId(tagValueId);
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertEquals(1L, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertEquals(1001L, pageResult.getList().get(0).getProductSpuId());
    }

    private void insertSpu(Long spuId, String spuCode) {
        spuTableMapper.insert(SpuTableDO.builder()
                .productSpuId(spuId)
                .productSpuCode(spuCode)
                .productSpuName(spuCode)
                .productBrand("brand")
                .categoryId(1L)
                .productOrigin("origin")
                .productManufacturer("manufacturer")
                .productSpecTemplate("{}")
                .productSpuStatus(1)
                .build());
    }

    private void insertSpuRelation(String objectId, Long tagValueId) {
        tagObjectRelationMapper.insert(TagObjectRelationDO.builder()
                .tenantId(1L)
                .domainType(DOMAIN_TYPE_PRODUCT)
                .objectType(OBJECT_TYPE_SPU)
                .objectId(objectId)
                .tagValueId(tagValueId)
                .sourceType(SOURCE_TYPE_MANUAL)
                .sourceRef("")
                .status(RELATION_STATUS_ENABLED)
                .effectiveTime(LocalDateTime.now())
                .uniqueDeleted(0L)
                .build());
    }

    private Long createL3Dimension() {
        Long l1Id = createDimension(ROOT_PARENT_ID, LEVEL_L1, "PRODUCT-L1", "product_l1");
        Long l2Id = createDimension(l1Id, LEVEL_L2, "PRODUCT-L2", "product_l2");
        return createDimension(l2Id, LEVEL_L3, "PRODUCT-L3", "product_l3");
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
