package cn.iocoder.yudao.module.ele.service;

import java.util.List;

public interface EleStoreInventorySkuScopeService {

    List<String> listStoreSkuScope(String storeId, String erpStoreCode);
}
