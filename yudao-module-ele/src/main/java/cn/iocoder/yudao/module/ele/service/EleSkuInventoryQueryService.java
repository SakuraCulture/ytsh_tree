package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryBatchQueryReqBO;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;

public interface EleSkuInventoryQueryService {

    EleSkuInventoryBatchQueryRespDTO queryBatch(EleSkuInventoryBatchQueryReqBO reqBO);
}
