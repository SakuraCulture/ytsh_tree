package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsSyncReqBO;
import cn.iocoder.yudao.module.ele.service.dto.EleStoreGoodsQueryRespDTO;

public interface EleStoreGoodsSyncService {

    void syncStoreGoods(EleStoreGoodsSyncReqBO reqBO);

    EleStoreGoodsQueryRespDTO queryStoreGoods(EleStoreGoodsQueryReqBO reqBO);

    EleStoreGoodsPageSyncResult queryAndSyncStoreGoods(EleStoreGoodsQueryReqBO reqBO, Boolean testMode);

    EleStoreGoodsPageSyncResult syncStoreGoodsPage(EleStoreGoodsQueryReqBO reqBO, Boolean testMode);
}
