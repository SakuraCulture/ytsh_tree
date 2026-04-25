package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagDimensionSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagDimensionDO;
import jakarta.validation.Valid;

import java.util.List;

public interface TagDimensionService {

    Long createTagDimension(@Valid TagDimensionSaveReqVO createReqVO);

    void updateTagDimension(@Valid TagDimensionSaveReqVO updateReqVO);

    void deleteTagDimension(Long id);

    TagDimensionDO getTagDimension(Long id);

    List<TagDimensionDO> getTagDimensionList(String domainType, Long parentId, Integer level);

}
