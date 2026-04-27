package cn.iocoder.yudao.module.business.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSimpleRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSourceRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SpuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import cn.iocoder.yudao.module.business.service.tag.TagObjectRelationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.SPU_TABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.TAG_VALUE_DIMENSION_LEVEL_ERROR;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.TAG_VALUE_DISABLED;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.TAG_VALUE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.TAG_VALUE_NOT_PRODUCT_DOMAIN;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.DOMAIN_TYPE_PRODUCT;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.LEVEL_L3;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.OBJECT_TYPE_SPU;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.STATUS_ENABLED;

@Service
@Validated
public class ProductSpuTagServiceImpl implements ProductSpuTagService {

    @Resource
    private SpuTableMapper spuTableMapper;
    @Resource
    private TagValueMapper tagValueMapper;
    @Resource
    private TagDimensionMapper tagDimensionMapper;
    @Resource
    private TagObjectRelationService tagObjectRelationService;

    @Override
    public void saveManualTags(ProductSpuTagSaveReqVO reqVO) {
        validateSpuExists(reqVO.getProductSpuId());
        List<Long> tagValueIds = reqVO.getTagValueIds() == null ? List.of() : reqVO.getTagValueIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        validateTagValues(tagValueIds);
        tagObjectRelationService.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, reqVO.getProductSpuId(), tagValueIds);
    }

    @Override
    public List<ProductSpuTagRespVO> getTagList(Long productSpuId) {
        validateSpuExists(productSpuId);
        List<TagObjectRelationDO> relations = sortRelations(tagObjectRelationService.getActiveRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, productSpuId));
        return buildTagRespList(relations, buildTagLookupContext(relations));
    }

    @Override
    public List<ProductSpuTagSimpleRespVO> getSimpleTagList(Collection<Long> productSpuIds) {
        if (CollUtil.isEmpty(productSpuIds)) {
            return List.of();
        }
        List<Long> distinctProductSpuIds = productSpuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(distinctProductSpuIds)) {
            return List.of();
        }
        List<TagObjectRelationDO> relations = sortRelations(tagObjectRelationService.getActiveRelationsByObjectIds(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, distinctProductSpuIds));
        if (CollUtil.isEmpty(relations)) {
            return distinctProductSpuIds.stream().map(productSpuId -> {
                ProductSpuTagSimpleRespVO respVO = new ProductSpuTagSimpleRespVO();
                respVO.setProductSpuId(productSpuId);
                respVO.setTags(List.of());
                return respVO;
            }).toList();
        }
        TagLookupContext lookupContext = buildTagLookupContext(relations);
        Map<Long, List<TagObjectRelationDO>> relationMap = relations.stream()
                .collect(Collectors.groupingBy(TagObjectRelationDO::getObjectId, LinkedHashMap::new, Collectors.toList()));
        List<ProductSpuTagSimpleRespVO> results = new ArrayList<>();
        for (Long productSpuId : distinctProductSpuIds) {
            ProductSpuTagSimpleRespVO respVO = new ProductSpuTagSimpleRespVO();
            respVO.setProductSpuId(productSpuId);
            respVO.setTags(buildTagRespList(relationMap.getOrDefault(productSpuId, List.of()), lookupContext));
            results.add(respVO);
        }
        return results;
    }

    private List<TagObjectRelationDO> sortRelations(List<TagObjectRelationDO> relations) {
        if (CollUtil.isEmpty(relations)) {
            return List.of();
        }
        return relations.stream()
                .sorted(Comparator.comparing(TagObjectRelationDO::getObjectId)
                        .thenComparing(TagObjectRelationDO::getTagValueId)
                        .thenComparing(TagObjectRelationDO::getId))
                .toList();
    }

    private void validateSpuExists(Long productSpuId) {
        SpuTableDO spuTable = spuTableMapper.selectById(productSpuId);
        if (spuTable == null) {
            throw exception(SPU_TABLE_NOT_EXISTS);
        }
    }

    private void validateTagValues(List<Long> tagValueIds) {
        for (Long tagValueId : tagValueIds) {
            TagValueDO tagValue = tagValueMapper.selectById(tagValueId);
            if (tagValue == null) {
                throw exception(TAG_VALUE_NOT_EXISTS);
            }
            if (!Objects.equals(tagValue.getStatus(), STATUS_ENABLED)) {
                throw exception(TAG_VALUE_DISABLED);
            }
            TagDimensionDO dimension = tagDimensionMapper.selectById(tagValue.getDimensionId());
            if (dimension == null) {
                throw exception(TAG_VALUE_NOT_EXISTS);
            }
            if (!Objects.equals(dimension.getDomainType(), DOMAIN_TYPE_PRODUCT)) {
                throw exception(TAG_VALUE_NOT_PRODUCT_DOMAIN);
            }
            if (!Objects.equals(dimension.getLevel(), LEVEL_L3)) {
                throw exception(TAG_VALUE_DIMENSION_LEVEL_ERROR);
            }
            if (!Objects.equals(dimension.getStatus(), STATUS_ENABLED)) {
                throw exception(TAG_VALUE_DISABLED);
            }
        }
    }

    private List<ProductSpuTagRespVO> buildTagRespList(List<TagObjectRelationDO> relations, TagLookupContext lookupContext) {
        if (CollUtil.isEmpty(relations)) {
            return List.of();
        }
        Map<Long, List<TagObjectRelationDO>> groupedRelations = relations.stream()
                .collect(Collectors.groupingBy(TagObjectRelationDO::getTagValueId, LinkedHashMap::new, Collectors.toList()));
        List<ProductSpuTagRespVO> results = new ArrayList<>();
        for (Map.Entry<Long, List<TagObjectRelationDO>> entry : groupedRelations.entrySet()) {
            TagValueDO tagValue = lookupContext.tagValueMap().get(entry.getKey());
            if (tagValue == null) {
                continue;
            }
            TagDimensionDO l3Dimension = lookupContext.dimensionMap().get(tagValue.getDimensionId());
            String dimensionPath = buildDimensionPath(l3Dimension, lookupContext.dimensionMap());
            if (dimensionPath == null) {
                continue;
            }
            ProductSpuTagRespVO respVO = new ProductSpuTagRespVO();
            respVO.setTagValueId(tagValue.getId());
            respVO.setTagValueCode(tagValue.getCode());
            respVO.setTagValueName(tagValue.getName());
            respVO.setDimensionPath(dimensionPath);
            respVO.setSources(buildSources(entry.getValue()));
            respVO.setSourceDetails(buildSourceDetails(entry.getValue()));
            results.add(respVO);
        }
        return results;
    }

    private TagLookupContext buildTagLookupContext(List<TagObjectRelationDO> relations) {
        if (CollUtil.isEmpty(relations)) {
            return new TagLookupContext(Collections.emptyMap(), Collections.emptyMap());
        }
        Set<Long> tagValueIds = relations.stream().map(TagObjectRelationDO::getTagValueId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, TagValueDO> tagValueMap = tagValueMapper.selectBatchIds(tagValueIds).stream()
                .collect(Collectors.toMap(TagValueDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Set<Long> dimensionIds = tagValueMap.values().stream().map(TagValueDO::getDimensionId).collect(Collectors.toCollection(LinkedHashSet::new));
        return new TagLookupContext(tagValueMap, loadDimensionMap(dimensionIds));
    }

    private Map<Long, TagDimensionDO> loadDimensionMap(Set<Long> dimensionIds) {
        if (CollUtil.isEmpty(dimensionIds)) {
            return Collections.emptyMap();
        }
        Map<Long, TagDimensionDO> dimensionMap = new LinkedHashMap<>();
        Set<Long> pending = new LinkedHashSet<>(dimensionIds);
        while (CollUtil.isNotEmpty(pending)) {
            List<TagDimensionDO> dimensions = tagDimensionMapper.selectBatchIds(pending);
            if (CollUtil.isEmpty(dimensions)) {
                break;
            }
            pending = new LinkedHashSet<>();
            for (TagDimensionDO dimension : dimensions) {
                dimensionMap.putIfAbsent(dimension.getId(), dimension);
                if (!Objects.equals(dimension.getParentId(), 0L) && !dimensionMap.containsKey(dimension.getParentId())) {
                    pending.add(dimension.getParentId());
                }
            }
        }
        return dimensionMap;
    }

    private String buildDimensionPath(TagDimensionDO l3Dimension, Map<Long, TagDimensionDO> dimensionMap) {
        if (l3Dimension == null) {
            return null;
        }
        TagDimensionDO l2Dimension = dimensionMap.get(l3Dimension.getParentId());
        if (l2Dimension == null) {
            return null;
        }
        TagDimensionDO l1Dimension = dimensionMap.get(l2Dimension.getParentId());
        if (l1Dimension == null) {
            return null;
        }
        return StrUtil.format("{} / {} / {}", l1Dimension.getName(), l2Dimension.getName(), l3Dimension.getName());
    }

    private List<String> buildSources(List<TagObjectRelationDO> relations) {
        return relations.stream()
                .map(TagObjectRelationDO::getSourceType)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<ProductSpuTagSourceRespVO> buildSourceDetails(List<TagObjectRelationDO> relations) {
        return relations.stream().map(relation -> {
            ProductSpuTagSourceRespVO detail = new ProductSpuTagSourceRespVO();
            detail.setSourceType(relation.getSourceType());
            detail.setSourceRef(relation.getSourceRef());
            detail.setStatus(relation.getStatus());
            detail.setEffectiveTime(relation.getEffectiveTime());
            detail.setExpireTime(relation.getExpireTime());
            return detail;
        }).toList();
    }

    private record TagLookupContext(Map<Long, TagValueDO> tagValueMap,
                                    Map<Long, TagDimensionDO> dimensionMap) {
    }

}
