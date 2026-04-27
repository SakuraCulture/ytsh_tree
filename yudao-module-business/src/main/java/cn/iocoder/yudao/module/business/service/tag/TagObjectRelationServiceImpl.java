package cn.iocoder.yudao.module.business.service.tag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagObjectRelationDO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagObjectRelationMapper;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagValueMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;

@Service
@Validated
public class TagObjectRelationServiceImpl implements TagObjectRelationService {

    @Resource
    private TagObjectRelationMapper tagObjectRelationMapper;

    @Resource
    private TagValueMapper tagValueMapper;

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveManualRelations(String domainType, String objectType, Long objectId, List<Long> tagValueIds) {
        saveRelations(domainType, objectType, objectId, tagValueIds, SOURCE_TYPE_MANUAL, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRuleRelations(String domainType, String objectType, Long objectId, String sourceRef, List<Long> tagValueIds) {
        saveRelations(domainType, objectType, objectId, tagValueIds, SOURCE_TYPE_RULE, sourceRef);
    }

    @Override
    public List<TagObjectRelationDO> getActiveRelations(String domainType, String objectType, Long objectId) {
        validateScope(domainType, objectType, objectId);
        return tagObjectRelationMapper.selectActiveList(domainType, objectType, objectId);
    }

    @Override
    public List<TagObjectRelationDO> getActiveRelationsByObjectIds(String domainType, String objectType, Collection<Long> objectIds) {
        validateDomainType(domainType);
        validateObjectType(objectType);
        if (CollUtil.isEmpty(objectIds)) {
            return List.of();
        }
        return tagObjectRelationMapper.selectActiveListByObjectIds(domainType, objectType, objectIds);
    }

    private void saveRelations(String domainType, String objectType, Long objectId, List<Long> tagValueIds,
                               String sourceType, String sourceRef) {
        validateScope(domainType, objectType, objectId);
        validateSourceType(sourceType);
        String normalizedSourceRef = normalizeSourceRef(sourceType, sourceRef);
        List<Long> dedupedTagValueIds = dedupeTagValueIds(tagValueIds);
        validateTagValues(dedupedTagValueIds);

        LocalDateTime now = LocalDateTime.now();
        List<TagObjectRelationDO> scopedRelations = tagObjectRelationMapper.selectListByObjectAndSource(
                domainType, objectType, objectId, sourceType, normalizedSourceRef);
        disableRemovedRelations(scopedRelations, dedupedTagValueIds, now);
        upsertTargetRelations(domainType, objectType, objectId, dedupedTagValueIds, sourceType, normalizedSourceRef, now);
    }

    private void validateScope(String domainType, String objectType, Long objectId) {
        validateDomainType(domainType);
        validateObjectType(objectType);
        if (objectId == null) {
            throw exception(TAG_OBJECT_TYPE_INVALID);
        }
    }

    private void validateDomainType(String domainType) {
        if (!Objects.equals(domainType, DOMAIN_TYPE_PRODUCT)) {
            throw exception(TAG_DOMAIN_TYPE_INVALID);
        }
    }

    private void validateObjectType(String objectType) {
        if (!OBJECT_TYPES.contains(objectType)) {
            throw exception(TAG_OBJECT_TYPE_INVALID);
        }
    }

    private void validateSourceType(String sourceType) {
        if (!SOURCE_TYPES.contains(sourceType)) {
            throw exception(TAG_SOURCE_TYPE_INVALID);
        }
    }

    private String normalizeSourceRef(String sourceType, String sourceRef) {
        if (Objects.equals(sourceType, SOURCE_TYPE_MANUAL)) {
            return "";
        }
        String normalized = StrUtil.trimToEmpty(sourceRef);
        if (StrUtil.isBlank(normalized)) {
            throw exception(TAG_SOURCE_REF_REQUIRED);
        }
        return normalized;
    }

    private List<Long> dedupeTagValueIds(List<Long> tagValueIds) {
        if (CollUtil.isEmpty(tagValueIds)) {
            return List.of();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long tagValueId : tagValueIds) {
            if (tagValueId != null) {
                uniqueIds.add(tagValueId);
            }
        }
        return List.copyOf(uniqueIds);
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
            if (dimension == null || !Objects.equals(dimension.getDomainType(), DOMAIN_TYPE_PRODUCT)) {
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

    private void disableRemovedRelations(List<TagObjectRelationDO> scopedRelations, List<Long> targetTagValueIds, LocalDateTime expireTime) {
        Set<Long> targetIdSet = Set.copyOf(targetTagValueIds);
        for (TagObjectRelationDO relation : scopedRelations) {
            if (targetIdSet.contains(relation.getTagValueId())) {
                continue;
            }
            if (Objects.equals(relation.getStatus(), RELATION_STATUS_DISABLED)) {
                continue;
            }
            TagObjectRelationDO updateObj = new TagObjectRelationDO();
            updateObj.setId(relation.getId());
            updateObj.setStatus(RELATION_STATUS_DISABLED);
            updateObj.setExpireTime(expireTime);
            tagObjectRelationMapper.updateById(updateObj);
        }
    }

    private void upsertTargetRelations(String domainType, String objectType, Long objectId, List<Long> tagValueIds,
                                       String sourceType, String sourceRef, LocalDateTime effectiveTime) {
        for (Long tagValueId : tagValueIds) {
            TagObjectRelationDO existing = tagObjectRelationMapper.selectByBiz(domainType, objectType, objectId, tagValueId,
                    sourceType, sourceRef);
            if (existing == null) {
                tagObjectRelationMapper.insert(TagObjectRelationDO.builder()
                        .domainType(domainType)
                        .objectType(objectType)
                        .objectId(objectId)
                        .tagValueId(tagValueId)
                        .sourceType(sourceType)
                        .sourceRef(sourceRef)
                        .status(RELATION_STATUS_ENABLED)
                        .effectiveTime(effectiveTime)
                        .uniqueDeleted(0L)
                        .build());
                continue;
            }
            TagObjectRelationDO updateObj = new TagObjectRelationDO();
            updateObj.setId(existing.getId());
            updateObj.setStatus(RELATION_STATUS_ENABLED);
            updateObj.setEffectiveTime(effectiveTime);
            updateObj.setExpireTime(null);
            tagObjectRelationMapper.updateById(updateObj);
        }
    }

}
