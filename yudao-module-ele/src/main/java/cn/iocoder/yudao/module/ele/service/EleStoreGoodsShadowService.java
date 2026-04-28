package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowRespVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;

public interface EleStoreGoodsShadowService {

    EleStoreGoodsShadowDO upsertFromSync(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                         String matchedProductSkuId, String mergedStoreProductId);

    void markMerged(Long shadowId, String matchedProductSkuId, String mergedStoreProductId);

    PageResult<EleStoreGoodsShadowRespVO> getShadowPage(EleStoreGoodsShadowPageReqVO reqVO);

    EleStoreGoodsShadowRespVO getShadow(Long id);

    void ignore(Long id);

    void mergeManually(Long id, String matchedProductSkuId);
}
