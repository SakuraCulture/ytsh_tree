package cn.iocoder.yudao.module.business.service.product;

import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSimpleRespVO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

public interface ProductSpuTagService {

    void saveManualTags(@Valid ProductSpuTagSaveReqVO reqVO);

    List<ProductSpuTagRespVO> getTagList(Long productSpuId);

    List<ProductSpuTagSimpleRespVO> getSimpleTagList(Collection<Long> productSpuIds);

}
