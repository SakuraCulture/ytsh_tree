package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;

public interface EleStoreInventoryIngestService {

    String PERSIST_STATUS_FORMAL = "FORMAL";
    String PERSIST_STATUS_SHADOW = "SHADOW";
    String MATCH_STATUS_SKU_NOT_MATCHED = "SKU_NOT_MATCHED";
    String REASON_CODE_SKU_NOT_FOUND = "SKU_NOT_FOUND";

    EleStoreInventoryIngestResultBO ingest(EleStoreInventoryIngestRowBO row);
}
