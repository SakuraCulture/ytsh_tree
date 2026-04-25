package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.module.business.service.store.bo.StoreProductSyncUpsertReqBO;

public interface StoreProductSyncWriteService {

    String upsertStoreProduct(StoreProductSyncUpsertReqBO reqBO);
}
