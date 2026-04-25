package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import cn.iocoder.yudao.module.business.dal.mysql.tag.TagDimensionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.business.enums.tag.TagConstants.*;

@Service
@Validated
public class TagDimensionServiceImpl implements TagDimensionService {

    @Resource
    private TagDimensionMapper tagDimensionMapper;

    @Override
    public Long createTagDimension(TagDimensionSaveReqVO createReqVO) {
        validateDomainType(createReqVO.getDomainType());
        validateStatus(createReqVO);
        validateLevelAndParent(createReqVO.getDomainType(), createReqVO.getLevel(), createReqVO.getParentId());
        validateCodeUnique(null, createReqVO.getDomainType(), createReqVO.getParentId(), createReqVO.getCode());

        TagDimensionDO tagDimension = BeanUtils.toBean(createReqVO, TagDimensionDO.class);
        tagDimension.setUniqueDeleted(0L);
        tagDimensionMapper.insert(tagDimension);
        return tagDimension.getId();
    }

    @Override
    public void updateTagDimension(TagDimensionSaveReqVO updateReqVO) {
        TagDimensionDO tagDimension = validateTagDimensionExists(updateReqVO.getId());
        validateDomainType(updateReqVO.getDomainType());
        validateStatus(updateReqVO);
        validateChildrenStructureUnchanged(tagDimension, updateReqVO);
        validateLevelAndParent(updateReqVO.getDomainType(), updateReqVO.getLevel(), updateReqVO.getParentId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getDomainType(), updateReqVO.getParentId(), updateReqVO.getCode());

        TagDimensionDO updateObj = new TagDimensionDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setDomainType(updateReqVO.getDomainType());
        updateObj.setParentId(updateReqVO.getParentId());
        updateObj.setLevel(updateReqVO.getLevel());
        updateObj.setName(updateReqVO.getName());
        updateObj.setCode(updateReqVO.getCode());
        updateObj.setSort(updateReqVO.getSort());
        updateObj.setStatus(updateReqVO.getStatus());
        updateObj.setDescription(updateReqVO.getDescription());
        tagDimensionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagDimension(Long id) {
        validateTagDimensionExists(id);
        if (tagDimensionMapper.selectCountByParentId(id) > 0) {
            throw exception(TAG_DIMENSION_HAS_CHILDREN);
        }

        TagDimensionDO updateObj = new TagDimensionDO();
        updateObj.setId(id);
        updateObj.setUniqueDeleted(id);
        tagDimensionMapper.updateById(updateObj);
        tagDimensionMapper.deleteById(id);
    }

    @Override
    public TagDimensionDO getTagDimension(Long id) {
        return tagDimensionMapper.selectById(id);
    }

    @Override
    public List<TagDimensionDO> getTagDimensionList(String domainType, Long parentId, Integer level) {
        return tagDimensionMapper.selectList(domainType, parentId, level);
    }

    private TagDimensionDO validateTagDimensionExists(Long id) {
        TagDimensionDO tagDimension = tagDimensionMapper.selectById(id);
        if (tagDimension == null) {
            throw exception(TAG_DIMENSION_NOT_EXISTS);
        }
        return tagDimension;
    }

    private void validateDomainType(String domainType) {
        if (!DOMAIN_TYPES.contains(domainType)) {
            throw exception(TAG_DOMAIN_TYPE_INVALID);
        }
    }

    private void validateStatus(TagDimensionSaveReqVO reqVO) {
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(STATUS_ENABLED);
            return;
        }
        if (!Objects.equals(reqVO.getStatus(), STATUS_DISABLED) && !Objects.equals(reqVO.getStatus(), STATUS_ENABLED)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
    }

    private void validateChildrenStructureUnchanged(TagDimensionDO tagDimension, TagDimensionSaveReqVO updateReqVO) {
        if (tagDimensionMapper.selectCountByParentId(tagDimension.getId()) <= 0) {
            return;
        }
        if (!Objects.equals(tagDimension.getDomainType(), updateReqVO.getDomainType())
                || !Objects.equals(tagDimension.getParentId(), updateReqVO.getParentId())
                || !Objects.equals(tagDimension.getLevel(), updateReqVO.getLevel())) {
            throw exception(TAG_DIMENSION_HAS_CHILDREN);
        }
    }

    private void validateLevelAndParent(String domainType, Integer level, Long parentId) {
        if (!Objects.equals(level, LEVEL_L1) && !Objects.equals(level, LEVEL_L2) && !Objects.equals(level, LEVEL_L3)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
        if (Objects.equals(level, LEVEL_L1)) {
            if (!Objects.equals(parentId, ROOT_PARENT_ID)) {
                throw exception(TAG_DIMENSION_LEVEL_ERROR);
            }
            return;
        }

        TagDimensionDO parent = tagDimensionMapper.selectById(parentId);
        if (parent == null) {
            throw exception(TAG_DIMENSION_PARENT_NOT_EXISTS);
        }
        if (!Objects.equals(parent.getDomainType(), domainType)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
        if (Objects.equals(level, LEVEL_L2) && !Objects.equals(parent.getLevel(), LEVEL_L1)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
        if (Objects.equals(level, LEVEL_L3) && !Objects.equals(parent.getLevel(), LEVEL_L2)) {
            throw exception(TAG_DIMENSION_LEVEL_ERROR);
        }
    }

    private void validateCodeUnique(Long id, String domainType, Long parentId, String code) {
        TagDimensionDO tagDimension = tagDimensionMapper.selectByDomainTypeAndParentIdAndCode(domainType, parentId, code);
        if (tagDimension == null) {
            return;
        }
        if (id == null || !Objects.equals(tagDimension.getId(), id)) {
            throw exception(TAG_DIMENSION_CODE_EXISTS);
        }
    }

}
