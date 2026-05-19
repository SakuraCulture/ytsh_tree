package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryGovernancePoolDO;

public interface EleSkuInventoryGovernanceService {

    Long createOrRefresh(EleStoreInventoryGovernancePoolDO governancePool);
}
