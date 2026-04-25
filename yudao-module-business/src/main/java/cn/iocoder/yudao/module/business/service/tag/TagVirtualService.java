package cn.iocoder.yudao.module.business.service.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.tag.vo.TagVirtualSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.tag.TagVirtualDO;
import jakarta.validation.Valid;

public interface TagVirtualService {

    Long createTagVirtual(@Valid TagVirtualSaveReqVO createReqVO);

    void updateTagVirtual(@Valid TagVirtualSaveReqVO updateReqVO);

    void deleteTagVirtual(Long id);

    TagVirtualDO getTagVirtual(Long id);

    PageResult<TagVirtualDO> getTagVirtualPage(TagVirtualPageReqVO pageReqVO);

}
