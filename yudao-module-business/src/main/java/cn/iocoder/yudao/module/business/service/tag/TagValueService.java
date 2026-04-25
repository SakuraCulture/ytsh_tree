package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValuePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagValueSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagValueDO;
import jakarta.validation.Valid;

import java.util.List;

public interface TagValueService {

    Long createTagValue(@Valid TagValueSaveReqVO createReqVO);

    void updateTagValue(@Valid TagValueSaveReqVO updateReqVO);

    void deleteTagValue(Long id);

    TagValueDO getTagValue(Long id);

    PageResult<TagValueDO> getTagValuePage(TagValuePageReqVO pageReqVO);

    List<TagValueDO> getTagValueListByDimensionId(Long dimensionId);

    TagValueImportRespVO importTagValueList(List<TagValueImportReqVO> importList, boolean updateSupport);

}
