package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagBatchRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagBatchSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import cn.iocoder.yudao.module.business.service.tag.TagObjectRelationServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.STORE_PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.DOMAIN_TYPE_PRODUCT;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L1;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L2;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L3;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.SOURCE_TYPE_MANUAL;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.STATUS_ENABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.TAG_METHOD_MANUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({StoreProductTagServiceImpl.class, TagObjectRelationServiceImpl.class})
class StoreProductTagServiceImplTest extends BaseDbUnitTest {

    @Resource
    private StoreProductTagService service;

    @Resource
    private StoreProductMapper storeProductMapper;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Test
    void saveManualTags_success() {
        insertStoreProduct("SP-001", "STORE-001", "SKU-001");
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, "store_tag_1");
        Long tagValueId2 = createTagValue(dimensionId, "store_tag_2");

        StoreProductTagSaveReqVO reqVO = new StoreProductTagSaveReqVO();
        reqVO.setStoreProductId("SP-001");
        reqVO.setTagValueIds(List.of(tagValueId1, tagValueId2));

        service.saveManualTags(reqVO);

        List<StoreProductTagRespVO> tags = service.getTagList("SP-001");
        assertEquals(2, tags.size());
        assertEquals(List.of(tagValueId1, tagValueId2), tags.stream().map(StoreProductTagRespVO::getTagValueId).sorted().toList());
        assertTrue(tags.stream().allMatch(item -> item.getSources().contains(SOURCE_TYPE_MANUAL)));
    }

    @Test
    void saveManualTags_shouldRejectMissingStoreProduct() {
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "store_tag_missing");

        StoreProductTagSaveReqVO reqVO = new StoreProductTagSaveReqVO();
        reqVO.setStoreProductId("shadow-only");
        reqVO.setTagValueIds(List.of(tagValueId));

        assertServiceException(() -> service.saveManualTags(reqVO), STORE_PRODUCT_NOT_EXISTS);
    }

    @Test
    void getSimpleTagList_shouldReturnTaggedAndEmptyRows() {
        insertStoreProduct("SP-001", "STORE-001", "SKU-001");
        insertStoreProduct("SP-002", "STORE-001", "SKU-002");
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "store_tag_simple");

        StoreProductTagSaveReqVO reqVO = new StoreProductTagSaveReqVO();
        reqVO.setStoreProductId("SP-001");
        reqVO.setTagValueIds(List.of(tagValueId));
        service.saveManualTags(reqVO);

        List<StoreProductTagSimpleRespVO> result = service.getSimpleTagList(List.of("SP-001", "SP-002"));

        assertEquals(2, result.size());
        assertEquals("SP-001", result.get(0).getStoreProductId());
        assertEquals(1, result.get(0).getTags().size());
        assertEquals(tagValueId, result.get(0).getTags().get(0).getTagValueId());
        assertEquals("SP-002", result.get(1).getStoreProductId());
        assertTrue(result.get(1).getTags().isEmpty());
    }

    @Test
    void saveManualTagsBatch_success() {
        insertStoreProduct("SP-001", "STORE-001", "SKU-001");
        insertStoreProduct("SP-002", "STORE-001", "SKU-002");
        Long dimensionId = createL3Dimension();
        Long tagValueId1 = createTagValue(dimensionId, "store_batch_1");
        Long tagValueId2 = createTagValue(dimensionId, "store_batch_2");

        StoreProductTagBatchSaveReqVO reqVO = new StoreProductTagBatchSaveReqVO();
        reqVO.setStoreProductIds(List.of("SP-001", "SP-002"));
        reqVO.setTagValueIds(List.of(tagValueId1, tagValueId2));

        StoreProductTagBatchRespVO respVO = service.saveManualTagsBatch(reqVO);

        assertEquals(2, respVO.getSuccessCount());
        assertEquals(0, respVO.getFailureCount());
        assertTrue(respVO.getFailureDetails().isEmpty());
        assertEquals(2, service.getTagList("SP-001").size());
        assertEquals(2, service.getTagList("SP-002").size());
    }

    @Test
    void saveManualTagsBatch_shouldReturnPartialFailureDetails() {
        insertStoreProduct("SP-001", "STORE-001", "SKU-001");
        Long dimensionId = createL3Dimension();
        Long tagValueId = createTagValue(dimensionId, "store_batch_partial");

        StoreProductTagBatchSaveReqVO reqVO = new StoreProductTagBatchSaveReqVO();
        reqVO.setStoreProductIds(List.of("SP-001", "SP-404"));
        reqVO.setTagValueIds(List.of(tagValueId));

        StoreProductTagBatchRespVO respVO = service.saveManualTagsBatch(reqVO);

        assertEquals(1, respVO.getSuccessCount());
        assertEquals(1, respVO.getFailureCount());
        assertEquals(1, respVO.getFailureDetails().size());
        assertEquals("SP-404", respVO.getFailureDetails().get(0).getObjectId());
        assertEquals(1, service.getTagList("SP-001").size());
    }

    private void insertStoreProduct(String storeProductId, String storeId, String productSkuId) {
        storeProductMapper.insert(StoreProductDO.builder()
                .storeProductId(storeProductId)
                .storeId(storeId)
                .productSkuId(productSkuId)
                .storeProductOwnership("入店")
                .storeProductPosStatus("1")
                .storeProductIsActive(1)
                .build());
    }

    private Long createL3Dimension() {
        Long l1Id = createDimension(0L, LEVEL_L1, "PRODUCT-L1", "product_l1");
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
