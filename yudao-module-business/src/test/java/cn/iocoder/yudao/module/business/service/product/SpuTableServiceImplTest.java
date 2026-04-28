package cn.iocoder.yudao.module.business.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.SkuTableAggregateRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.SpuTableAggregateRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.SpuTablePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.product.SpuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.product.UpcTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagObjectRelationMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import cn.iocoder.yudao.module.business.service.tag.TagObjectRelationServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.DOMAIN_TYPE_PRODUCT;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L1;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L2;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L3;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.OBJECT_TYPE_SPU;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.RELATION_STATUS_DISABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.RELATION_STATUS_ENABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.ROOT_PARENT_ID;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.SOURCE_TYPE_MANUAL;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.STATUS_ENABLED;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.TAG_METHOD_MANUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({SpuTableServiceImpl.class, ProductSpuTagServiceImpl.class, TagObjectRelationServiceImpl.class})
class SpuTableServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SpuTableService spuTableService;
    @Resource
    private SpuTableMapper spuTableMapper;
    @Resource
    private SkuTableMapper skuTableMapper;
    @Resource
    private UpcTableMapper upcTableMapper;
    @Resource
    private TagDimensionMapper tagDimensionMapper;
    @Resource
    private TagValueMapper tagValueMapper;
    @Resource
    private TagObjectRelationMapper tagObjectRelationMapper;

    @Test
    void getSpuTablePage_whenTagValueIdPresent_shouldOnlyReturnMatchedSpu() {
        Long spuId1 = createSpu(2001L);
        Long spuId2 = createSpu(2002L);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "人群属性", "高价值带");
        Long targetTagValueId = createTagValue(l3Id, STATUS_ENABLED, "high_value", "高价值商品");
        Long otherTagValueId = createTagValue(l3Id, STATUS_ENABLED, "normal_value", "普通商品");
        createEnabledRelation(spuId1, targetTagValueId);
        createEnabledRelation(spuId2, otherTagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setTagValueId(targetTagValueId);

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        assertEquals(spuId1, pageResult.getList().get(0).getProductSpuId());
    }

    @Test
    void getSpuTablePage_whenTagValueIdAndSkuFilterPresent_shouldApplyIntersection() {
        Long spuId1 = createSpu(2003L);
        Long spuId2 = createSpu(2004L);
        Long spuId3 = createSpu(2005L);
        createSku(spuId1, "SKU-MATCH-1", "白色款");
        createSku(spuId2, "SKU-MATCH-2", "白色款");
        createSku(spuId3, "SKU-OTHER", "黑色款");
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "价格属性", "价格带");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "mid_price", "中价格带");
        createEnabledRelation(spuId1, tagValueId);
        createEnabledRelation(spuId3, tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductSkuName("白色款");
        reqVO.setTagValueId(tagValueId);

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        assertEquals(spuId1, pageResult.getList().get(0).getProductSpuId());
    }

    @Test
    void getSpuTablePage_whenTagValueIdHasNoMatch_shouldReturnEmpty() {
        Long spuId = createSpu(2006L);
        createSku(spuId, "SKU-EMPTY", "测试款");
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "场景属性", "露营场景");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "camping", "露营");

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setTagValueId(tagValueId);

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertTrue(pageResult.getList().isEmpty());
        assertEquals(0L, pageResult.getTotal());
    }

    @Test
    void getSpuTablePage_whenSkuAndTagHaveNoIntersection_shouldReturnEmpty() {
        Long spuId1 = createSpu(2007L);
        Long spuId2 = createSpu(2008L);
        createSku(spuId1, "SKU-WHITE", "白色款");
        createSku(spuId2, "SKU-BLACK", "黑色款");
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "功能属性", "功能角色");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "role_a", "角色A");
        createEnabledRelation(spuId2, tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductSkuName("白色款");
        reqVO.setTagValueId(tagValueId);

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertTrue(pageResult.getList().isEmpty());
        assertEquals(0L, pageResult.getTotal());
    }

    @Test
    void getSpuTablePage_whenSkuFilterPresentWithoutTagValueId_shouldKeepOriginalSkuFiltering() {
        Long spuId1 = createSpu(2009L);
        Long spuId2 = createSpu(2010L);
        createSku(spuId1, "SKU-RED", "红色款");
        createSku(spuId2, "SKU-BLUE", "蓝色款");

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductSkuName("红色款");

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        assertEquals(spuId1, pageResult.getList().get(0).getProductSpuId());
    }

    @Test
    void getSpuTablePage_whenMatchedRelationDisabled_shouldIgnoreDisabledRelation() {
        Long spuId = createSpu(2011L);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "功能属性", "功能角色");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "role_a", "角色A");
        createDisabledRelation(spuId, tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setTagValueId(tagValueId);

        PageResult<SpuTableDO> pageResult = spuTableService.getSpuTablePage(reqVO);

        assertTrue(pageResult.getList().isEmpty());
    }

    @Test
    void getSpuTableAggregatePage_shouldAssembleTagsSkuAndUpc() {
        Long spuId1 = createSpu(2012L);
        Long spuId2 = createSpu(2013L);
        createSku(spuId1, "SKU-B", "第二个SKU");
        createSku(spuId1, "SKU-A", "第一个SKU");
        createSku(spuId2, "SKU-C", "另一个SPU的SKU");
        List<SkuTableDO> spu1Skus = skuTableMapper.selectListByProductSpuId(spuId1);
        Long skuId1 = spu1Skus.get(0).getProductSkuId();
        Long skuId2 = spu1Skus.get(1).getProductSkuId();
        createUpc(skuId2, "EAN-13", "6900000000002", 1, 1);
        createUpc(skuId2, "UPC-A", "012345678905", 0, 1);
        createUpc(skuId1, "EAN-13", "6900000000001", 1, 1);
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "适用人群", "年龄层");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "adult", "成人");
        createEnabledRelation(spuId1, tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        PageResult<SpuTableAggregateRespVO> pageResult = spuTableService.getSpuTableAggregatePage(reqVO);

        SpuTableAggregateRespVO target = pageResult.getList().stream()
                .filter(item -> spuId1.equals(item.getProductSpuId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, target.getTags().size());
        assertEquals(tagValueId, target.getTags().get(0).getTagValueId());
        assertEquals(2, target.getSkuTables().size());
        assertEquals(skuId1, target.getSkuTables().get(0).getProductSkuId());
        assertEquals(skuId2, target.getSkuTables().get(1).getProductSkuId());
        assertEquals(1, target.getSkuTables().get(0).getUpcTables().size());
        assertEquals("6900000000001", target.getSkuTables().get(0).getUpcTables().get(0).getProductUpcValue());
        assertEquals(2, target.getSkuTables().get(1).getUpcTables().size());
        assertEquals("6900000000002", target.getSkuTables().get(1).getUpcTables().get(0).getProductUpcValue());
        assertEquals("012345678905", target.getSkuTables().get(1).getUpcTables().get(1).getProductUpcValue());
    }

    @Test
    void getSpuTableAggregatePage_whenChildrenMissing_shouldReturnEmptyArrays() {
        Long spuId = createSpu(2014L);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductSpuCode("SPU-" + spuId);

        PageResult<SpuTableAggregateRespVO> pageResult = spuTableService.getSpuTableAggregatePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        SpuTableAggregateRespVO respVO = pageResult.getList().get(0);
        assertTrue(respVO.getTags().isEmpty());
        assertTrue(respVO.getSkuTables().isEmpty());
    }

    @Test
    void getSpuTableAggregatePage_whenTagValueIdPresent_shouldOnlyReturnMatchedSpu() {
        Long spuId1 = createSpu(2015L);
        Long spuId2 = createSpu(2016L);
        createSku(spuId1, "SKU-TAG-1", "标签命中商品");
        createSku(spuId2, "SKU-TAG-2", "其他商品");
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "人群属性", "高价值带");
        Long targetTagValueId = createTagValue(l3Id, STATUS_ENABLED, "high_value_aggregate", "高价值商品");
        Long otherTagValueId = createTagValue(l3Id, STATUS_ENABLED, "normal_value_aggregate", "普通商品");
        createEnabledRelation(spuId1, targetTagValueId);
        createEnabledRelation(spuId2, otherTagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setTagValueId(targetTagValueId);

        PageResult<SpuTableAggregateRespVO> pageResult = spuTableService.getSpuTableAggregatePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        SpuTableAggregateRespVO spu = pageResult.getList().get(0);
        assertEquals(spuId1, spu.getProductSpuId());
        assertEquals(1, spu.getTags().size());
        assertEquals(targetTagValueId, spu.getTags().get(0).getTagValueId());
        assertEquals(1, spu.getSkuTables().size());
        assertEquals("SKU-TAG-1", spu.getSkuTables().get(0).getProductSkuCode());
    }

    @Test
    void getSpuTableAggregatePage_whenTagValueIdAndSkuFilterPresent_shouldApplyIntersection() {
        Long spuId1 = createSpu(2017L);
        Long spuId2 = createSpu(2018L);
        Long spuId3 = createSpu(2019L);
        createSku(spuId1, "SKU-MATCH-AGG-1", "白色款");
        createSku(spuId2, "SKU-MATCH-AGG-2", "白色款");
        createSku(spuId3, "SKU-OTHER-AGG", "黑色款");
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "价格属性", "价格带");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "mid_price_aggregate", "中价格带");
        createEnabledRelation(spuId1, tagValueId);
        createEnabledRelation(spuId3, tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductSkuName("白色款");
        reqVO.setTagValueId(tagValueId);

        PageResult<SpuTableAggregateRespVO> pageResult = spuTableService.getSpuTableAggregatePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        SpuTableAggregateRespVO spu = pageResult.getList().get(0);
        assertEquals(spuId1, spu.getProductSpuId());
        assertEquals(1, spu.getTags().size());
        assertEquals(tagValueId, spu.getTags().get(0).getTagValueId());
        assertEquals(1, spu.getSkuTables().size());
        assertEquals("SKU-MATCH-AGG-1", spu.getSkuTables().get(0).getProductSkuCode());
    }

    @Test
    void getSpuTableAggregatePage_whenSkuAndTagHaveNoIntersection_shouldReturnEmpty() {
        Long spuId1 = createSpu(2020L);
        Long spuId2 = createSpu(2021L);
        createSku(spuId1, "SKU-WHITE-AGG", "白色款");
        createSku(spuId2, "SKU-BLACK-AGG", "黑色款");
        Long l3Id = createL3Dimension(DOMAIN_TYPE_PRODUCT, "商品属性", "功能属性", "功能角色");
        Long tagValueId = createTagValue(l3Id, STATUS_ENABLED, "role_a_aggregate", "角色A");
        createEnabledRelation(spuId2, tagValueId);

        SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductSkuName("白色款");
        reqVO.setTagValueId(tagValueId);

        PageResult<SpuTableAggregateRespVO> pageResult = spuTableService.getSpuTableAggregatePage(reqVO);

        assertTrue(pageResult.getList().isEmpty());
        assertEquals(0L, pageResult.getTotal());
    }

    @Test
    void productSpecTemplate_shouldRoundTripJsonStringValues() {
        spuTableMapper.insert(SpuTableDO.builder()
                .productSpuId(2030L)
                .productSpuCode("SPU-2030")
                .productSpuName("SPU2030")
                .productBrand("品牌A")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("厂商A")
                .productSpecTemplate("标准规格")
                .productSpuStatus(1)
                .build());
        spuTableMapper.insert(SpuTableDO.builder()
                .productSpuId(2031L)
                .productSpuCode("SPU-2031")
                .productSpuName("SPU2031")
                .productBrand("品牌A")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("厂商A")
                .productSpecTemplate("123")
                .productSpuStatus(1)
                .build());
        spuTableMapper.insert(SpuTableDO.builder()
                .productSpuId(2032L)
                .productSpuCode("SPU-2032")
                .productSpuName("SPU2032")
                .productBrand("品牌A")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("厂商A")
                .productSpecTemplate("\"标准规格\"")
                .productSpuStatus(1)
                .build());
        spuTableMapper.insert(SpuTableDO.builder()
                .productSpuId(2033L)
                .productSpuCode("SPU-2033")
                .productSpuName("SPU2033")
                .productBrand("品牌A")
                .categoryId(1L)
                .productOrigin("中国")
                .productManufacturer("厂商A")
                .productSpecTemplate("")
                .productSpuStatus(1)
                .build());

        SpuTableDO textSpu = spuTableMapper.selectById(2030L);
        SpuTableDO numericTextSpu = spuTableMapper.selectById(2031L);
        SpuTableDO quotedTextSpu = spuTableMapper.selectById(2032L);
        SpuTableDO emptyTextSpu = spuTableMapper.selectById(2033L);

        assertEquals("标准规格", textSpu.getProductSpecTemplate());
        assertEquals("123", numericTextSpu.getProductSpecTemplate());
        assertEquals("\"标准规格\"", quotedTextSpu.getProductSpecTemplate());
        assertEquals("", emptyTextSpu.getProductSpecTemplate());
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

    private void createSku(Long spuId, String skuCode, String skuName) {
        skuTableMapper.insert(SkuTableDO.builder()
                .productSpuId(spuId)
                .productSkuCode(skuCode)
                .productSkuName(skuName)
                .productSkuStatus(1)
                .build());
    }

    private void createUpc(Long skuId, String upcType, String upcValue, Integer isPrimary, Integer status) {
        upcTableMapper.insert(UpcTableDO.builder()
                .productSkuId(skuId)
                .productUpcType(upcType)
                .productUpcValue(upcValue)
                .productUpcIsPrimary(isPrimary)
                .productUpcStatus(status)
                .build());
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

    private void createEnabledRelation(Long spuId, Long tagValueId) {
        tagObjectRelationMapper.insert(TagObjectRelationDO.builder()
                .tenantId(1L)
                .domainType(DOMAIN_TYPE_PRODUCT)
                .objectType(OBJECT_TYPE_SPU)
                .objectId(spuId)
                .tagValueId(tagValueId)
                .sourceType(SOURCE_TYPE_MANUAL)
                .sourceRef("")
                .status(RELATION_STATUS_ENABLED)
                .uniqueDeleted(0L)
                .build());
    }

    private void createDisabledRelation(Long spuId, Long tagValueId) {
        tagObjectRelationMapper.insert(TagObjectRelationDO.builder()
                .tenantId(1L)
                .domainType(DOMAIN_TYPE_PRODUCT)
                .objectType(OBJECT_TYPE_SPU)
                .objectId(spuId)
                .tagValueId(tagValueId)
                .sourceType(SOURCE_TYPE_MANUAL)
                .sourceRef("")
                .status(RELATION_STATUS_DISABLED)
                .uniqueDeleted(0L)
                .build());
    }
}
