package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;

public interface EleStoreGoodsShadowService {

    EleStoreGoodsShadowDO upsertFromSync(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                         String matchedProductSkuId, String mergedStoreProductId);

    void markMerged(Long shadowId, String matchedProductSkuId, String mergedStoreProductId);
}
