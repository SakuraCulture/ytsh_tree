package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagBatchRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagBatchSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagSimpleRespVO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

public interface StoreProductTagService {

    void saveManualTags(@Valid StoreProductTagSaveReqVO reqVO);

    StoreProductTagBatchRespVO saveManualTagsBatch(@Valid StoreProductTagBatchSaveReqVO reqVO);

    List<StoreProductTagRespVO> getTagList(String storeProductId);

    List<StoreProductTagSimpleRespVO> getSimpleTagList(Collection<String> storeProductIds);

}
