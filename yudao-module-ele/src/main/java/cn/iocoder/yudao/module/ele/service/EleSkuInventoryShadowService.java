package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryShadowUpsertReqBO;

public interface EleSkuInventoryShadowService {

    EleStoreInventoryShadowDO upsert(EleSkuInventoryShadowUpsertReqBO reqBO, String matchStatus, String reasonCode);

    EleStoreInventoryShadowDO getByBizKey(Long platformId, String merchantCode, String erpStoreCode,
                                          String skuCode, String subSkuCode);
}
