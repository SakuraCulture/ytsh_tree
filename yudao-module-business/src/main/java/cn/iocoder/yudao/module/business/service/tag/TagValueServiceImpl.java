package cn.iocoder.yudao.module.business.service.tag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagSelectableValueRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValuePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;

@Service
@Validated
public class TagValueServiceImpl implements TagValueService {

    @Resource
    private TagValueMapper tagValueMapper;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Override
    public Long createTagValue(TagValueSaveReqVO createReqVO) {
        validateStatus(createReqVO);
        validateTagMethod(createReqVO.getTagMethod());
        validateDimensionIsL3(createReqVO.getDimensionId());
        validateCodeUnique(null, createReqVO.getDimensionId(), createReqVO.getCode());

        TagValueDO tagValue = BeanUtils.toBean(createReqVO, TagValueDO.class);
        tagValue.setUniqueDeleted(0L);
        tagValueMapper.insert(tagValue);
        return tagValue.getId();
    }

    @Override
    public void updateTagValue(TagValueSaveReqVO updateReqVO) {
        validateTagValueExists(updateReqVO.getId());
        validateStatus(updateReqVO);
        validateTagMethod(updateReqVO.getTagMethod());
        validateDimensionIsL3(updateReqVO.getDimensionId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getDimensionId(), updateReqVO.getCode());

        TagValueDO updateObj = BeanUtils.toBean(updateReqVO, TagValueDO.class);
        tagValueMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagValue(Long id) {
        validateTagValueExists(id);

        TagValueDO updateObj = new TagValueDO();
        updateObj.setId(id);
        updateObj.setUniqueDeleted(id);
        tagValueMapper.updateById(updateObj);
        tagValueMapper.deleteById(id);
    }

    @Override
    public TagValueDO getTagValue(Long id) {
        return tagValueMapper.selectById(id);
    }

    @Override
    public PageResult<TagValueDO> getTagValuePage(TagValuePageReqVO pageReqVO) {
        if (StrUtil.isBlank(pageReqVO.getDomainType())) {
            return tagValueMapper.selectPage(pageReqVO);
        }
        List<Long> dimensionIds = tagDimensionMapper.selectList(pageReqVO.getDomainType(), null, null).stream()
                .map(TagDimensionDO::getId)
                .toList();
        if (CollUtil.isEmpty(dimensionIds)) {
            return PageResult.empty();
        }
        return tagValueMapper.selectPage(pageReqVO, dimensionIds);
    }

    @Override
    public List<TagValueDO> getTagValueListByDimensionId(Long dimensionId) {
        return tagValueMapper.selectListByDimensionId(dimensionId);
    }

    @Override
    public List<TagSelectableValueRespVO> getSelectableTagValuesForObject(String objectType) {
        validateObjectType(objectType);
        List<TagDimensionDO> dimensions = tagDimensionMapper.selectList(DOMAIN_TYPE_PRODUCT, null, null);
        if (CollUtil.isEmpty(dimensions)) {
            return List.of();
        }
        LinkedHashMap<Long, TagDimensionDO> dimensionMap = new LinkedHashMap<>();
        List<Long> enabledL3DimensionIds = new ArrayList<>();
        for (TagDimensionDO dimension : dimensions) {
            dimensionMap.put(dimension.getId(), dimension);
            if (Objects.equals(dimension.getLevel(), LEVEL_L3) && Objects.equals(dimension.getStatus(), STATUS_ENABLED)) {
                enabledL3DimensionIds.add(dimension.getId());
            }
        }
        if (CollUtil.isEmpty(enabledL3DimensionIds)) {
            return List.of();
        }
        List<TagValueDO> tagValues = tagValueMapper.selectEnabledListByDimensionIds(enabledL3DimensionIds);
        if (CollUtil.isEmpty(tagValues)) {
            return List.of();
        }
        List<TagSelectableValueRespVO> results = new ArrayList<>(tagValues.size());
        for (TagValueDO tagValue : tagValues) {
            TagDimensionDO dimension = dimensionMap.get(tagValue.getDimensionId());
            if (dimension == null) {
                continue;
            }
            String dimensionPath = buildDimensionPath(dimension, dimensionMap);
            if (dimensionPath == null) {
                continue;
            }
            TagSelectableValueRespVO respVO = new TagSelectableValueRespVO();
            respVO.setTagValueId(tagValue.getId());
            respVO.setTagValueCode(tagValue.getCode());
            respVO.setTagValueName(tagValue.getName());
            respVO.setDimensionId(dimension.getId());
            respVO.setDimensionName(dimension.getName());
            respVO.setDimensionPath(dimensionPath);
            respVO.setStatus(tagValue.getStatus());
            results.add(respVO);
        }
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagValueImportRespVO importTagValueList(List<TagValueImportReqVO> importList, boolean updateSupport) {
        if (CollUtil.isEmpty(importList)) {
            throw exception(TAG_VALUE_IMPORT_LIST_IS_EMPTY);
        }

        TagValueImportRespVO respVO = TagValueImportRespVO.builder()
                .createNames(new ArrayList<>())
                .updateNames(new ArrayList<>())
                .failureNames(new LinkedHashMap<>())
                .build();

        for (int i = 0; i < importList.size(); i++) {
            TagValueImportReqVO item = importList.get(i);
            String rowName = buildFailureName(item.getTagValueName(), i);
            try {
                validateImportItem(item);
                Long l1Id = getOrCreateDimension(item.getDomainType(), ROOT_PARENT_ID, LEVEL_L1, item.getL1Name(), item.getL1Code(), updateSupport);
                Long l2Id = getOrCreateDimension(item.getDomainType(), l1Id, LEVEL_L2, item.getL2Name(), item.getL2Code(), updateSupport);
                Long l3Id = getOrCreateDimension(item.getDomainType(), l2Id, LEVEL_L3, item.getL3Name(), item.getL3Code(), updateSupport);

                TagValueDO existValue = tagValueMapper.selectByDimensionIdAndCode(l3Id, item.getTagValueCode());
                if (existValue == null) {
                    TagValueDO tagValue = buildImportTagValue(item, l3Id);
                    tagValue.setUniqueDeleted(0L);
                    tagValueMapper.insert(tagValue);
                    respVO.getCreateNames().add(item.getTagValueName());
                } else if (updateSupport) {
                    TagValueDO updateObj = buildImportTagValue(item, l3Id);
                    updateObj.setId(existValue.getId());
                    tagValueMapper.updateById(updateObj);
                    respVO.getUpdateNames().add(item.getTagValueName());
                }
            } catch (Exception e) {
                respVO.getFailureNames().put(rowName, e.getMessage());
            }
        }
        return respVO;
    }

    private TagValueDO validateTagValueExists(Long id) {
        TagValueDO tagValue = tagValueMapper.selectById(id);
        if (tagValue == null) {
            throw exception(TAG_VALUE_NOT_EXISTS);
        }
        return tagValue;
    }

    private void validateTagMethod(String tagMethod) {
        if (!TAG_METHODS.contains(tagMethod)) {
            throw exception(TAG_METHOD_INVALID);
        }
    }

    private void validateObjectType(String objectType) {
        if (!OBJECT_TYPES.contains(objectType)) {
            throw exception(TAG_OBJECT_TYPE_INVALID);
        }
    }

    private void validateStatus(TagValueSaveReqVO reqVO) {
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(STATUS_ENABLED);
            return;
        }
        validateStatusValue(reqVO.getStatus());
    }

    private void validateStatusValue(Integer status) {
        if (!Objects.equals(status, STATUS_DISABLED) && !Objects.equals(status, STATUS_ENABLED)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
    }

    private TagDimensionDO validateDimensionIsL3(Long dimensionId) {
        TagDimensionDO dimension = tagDimensionMapper.selectById(dimensionId);
        if (dimension == null) {
            throw exception(TAG_DIMENSION_NOT_EXISTS);
        }
        if (!Objects.equals(dimension.getLevel(), LEVEL_L3)) {
            throw exception(TAG_VALUE_DIMENSION_LEVEL_ERROR);
        }
        return dimension;
    }

    private void validateCodeUnique(Long id, Long dimensionId, String code) {
        TagValueDO tagValue = tagValueMapper.selectByDimensionIdAndCode(dimensionId, code);
        if (tagValue == null) {
            return;
        }
        if (id == null || !Objects.equals(tagValue.getId(), id)) {
            throw exception(TAG_VALUE_CODE_EXISTS);
        }
    }

    private void validateImportItem(TagValueImportReqVO item) {
        if (!DOMAIN_TYPES.contains(item.getDomainType())) {
            throw exception(TAG_DOMAIN_TYPE_INVALID);
        }
        validateImportRequiredField(item.getL1Name());
        validateImportRequiredField(item.getL1Code());
        validateImportRequiredField(item.getL2Name());
        validateImportRequiredField(item.getL2Code());
        validateImportRequiredField(item.getL3Name());
        validateImportRequiredField(item.getL3Code());
        validateImportRequiredField(item.getTagValueName());
        validateImportRequiredField(item.getTagValueCode());
        validateStatusValue(item.getStatus() == null ? STATUS_ENABLED : item.getStatus());
        validateTagMethod(item.getTagMethod());
    }

    private Long getOrCreateDimension(String domainType, Long parentId, Integer level, String name, String code, boolean updateSupport) {
        TagDimensionDO dimension = tagDimensionMapper.selectByDomainTypeAndParentIdAndCode(domainType, parentId, code);
        if (dimension == null) {
            TagDimensionDO createObj = new TagDimensionDO();
            createObj.setDomainType(domainType);
            createObj.setParentId(parentId);
            createObj.setLevel(level);
            createObj.setName(name);
            createObj.setCode(code);
            createObj.setSort(0);
            createObj.setStatus(STATUS_ENABLED);
            createObj.setUniqueDeleted(0L);
            tagDimensionMapper.insert(createObj);
            return createObj.getId();
        }
        if (updateSupport) {
            TagDimensionDO updateObj = new TagDimensionDO();
            updateObj.setId(dimension.getId());
            updateObj.setName(name);
            tagDimensionMapper.updateById(updateObj);
        }
        return dimension.getId();
    }

    private void validateImportRequiredField(String value) {
        if (StrUtil.isBlank(value)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
    }

    private String buildDimensionPath(TagDimensionDO l3Dimension, LinkedHashMap<Long, TagDimensionDO> dimensionMap) {
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

    private String buildFailureName(String tagValueName, int rowIndex) {
        return StrUtil.format("{}#{}", StrUtil.blankToDefault(tagValueName, "<blank>"), rowIndex + 1);
    }

    private TagValueDO buildImportTagValue(TagValueImportReqVO item, Long dimensionId) {
        TagValueDO tagValue = new TagValueDO();
        tagValue.setDimensionId(dimensionId);
        tagValue.setName(item.getTagValueName());
        tagValue.setCode(item.getTagValueCode());
        tagValue.setTagMethod(item.getTagMethod());
        tagValue.setDataSource(item.getDataSource());
        tagValue.setUpdateFrequency(item.getUpdateFrequency());
        tagValue.setLogicDescription(item.getLogicDescription());
        tagValue.setSort(item.getSort() == null ? 0 : item.getSort());
        tagValue.setStatus(item.getStatus() == null ? STATUS_ENABLED : item.getStatus());
        return tagValue;
    }

}
