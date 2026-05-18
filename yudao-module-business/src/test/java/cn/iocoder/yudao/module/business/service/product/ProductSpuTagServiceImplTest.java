package cn.iocoder.yudao.module.business.service.product;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagBatchRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagBatchSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSimpleRespVO;
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
import java.util.List;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({ProductSpuTagServiceImpl.class, TagObjectRelationServiceImpl.class})
class ProductSpuTagServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ProductSpuTagService productSpuTagService;

    @Resource
    private SpuTableMapper spuTableMapper;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Resource
    private TagObjectRelationMapper tagObjectRelationMapper;

    @Test
    void getTagList_shouldReadStringifiedSpuRelationObjectId() {
        insertSpu(1001L, "SPU-1001");
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "stringified_spu_tag");
        insertSpuRelation("1001", tagValueId);

        List<ProductSpuTagRespVO> tags = productSpuTagService.getTagList(1001L);

        assertEquals(1, tags.size());
        ProductSpuTagRespVO tag = tags.get(0);
        assertEquals(tagValueId, tag.getTagValueId());
        assertEquals("stringified_spu_tag", tag.getTagValueCode());
        assertEquals("stringified_spu_tag", tag.getTagValueName());
        assertEquals("PRODUCT-L1 / PRODUCT-L2 / PRODUCT-L3", tag.getDimensionPath());
        assertEquals(List.of(SOURCE_TYPE_MANUAL), tag.getSources());
        assertEquals(1, tag.getSourceDetails().size());
    }

    @Test
    void getSimpleTagList_shouldGroupByStringifiedSpuRelationObjectId() {
        insertSpu(1001L, "SPU-1001");
        insertSpu(1002L, "SPU-1002");
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "simple_stringified_spu_tag");
        insertSpuRelation("1001", tagValueId);

        List<ProductSpuTagSimpleRespVO> result = productSpuTagService.getSimpleTagList(List.of(1001L, 1002L));

        assertEquals(2, result.size());
        assertEquals(1001L, result.get(0).getProductSpuId());
        assertEquals(1, result.get(0).getTags().size());
        assertEquals(tagValueId, result.get(0).getTags().get(0).getTagValueId());
        assertEquals(1002L, result.get(1).getProductSpuId());
        assertTrue(result.get(1).getTags().isEmpty());
    }

    @Test
    void saveManualTagsBatch_success() {
        insertSpu(1001L, "SPU-1001");
        insertSpu(1002L, "SPU-1002");
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, "spu_batch_tag_1");
        Long tagValueId2 = createTagValue(dimensionId, "spu_batch_tag_2");

        ProductSpuTagBatchSaveReqVO reqVO = new ProductSpuTagBatchSaveReqVO();
        reqVO.setProductSpuIds(List.of(1001L, 1002L));
        reqVO.setTagValueIds(List.of(tagValueId1, tagValueId2));

        ProductSpuTagBatchRespVO respVO = productSpuTagService.saveManualTagsBatch(reqVO);

        assertEquals(2, respVO.getSuccessCount());
        assertEquals(0, respVO.getFailureCount());
        assertTrue(respVO.getFailureDetails().isEmpty());
        assertEquals(2, productSpuTagService.getTagList(1001L).size());
        assertEquals(2, productSpuTagService.getTagList(1002L).size());
    }

    @Test
    void saveManualTagsBatch_shouldReturnPartialFailureDetails() {
        insertSpu(1001L, "SPU-1001");
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "spu_batch_partial");

        ProductSpuTagBatchSaveReqVO reqVO = new ProductSpuTagBatchSaveReqVO();
        reqVO.setProductSpuIds(List.of(1001L, 9999L));
        reqVO.setTagValueIds(List.of(tagValueId));

        ProductSpuTagBatchRespVO respVO = productSpuTagService.saveManualTagsBatch(reqVO);

        assertEquals(1, respVO.getSuccessCount());
        assertEquals(1, respVO.getFailureCount());
        assertEquals(1, respVO.getFailureDetails().size());
        assertEquals("9999", respVO.getFailureDetails().get(0).getObjectId());
        assertEquals("SPU基础分类不存在", respVO.getFailureDetails().get(0).getReason());
        assertEquals(1, productSpuTagService.getTagList(1001L).size());
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
