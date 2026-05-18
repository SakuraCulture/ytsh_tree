package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryShadowMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class EleStoreInventorySkuScopeServiceImpl implements EleStoreInventorySkuScopeService {

    @Resource
    private StoreProductMapper storeProductMapper;
    @Resource
    private SkuTableMapper skuTableMapper;
    @Resource
    private EleStoreGoodsShadowMapper storeGoodsShadowMapper;
    @Resource
    private EleStoreInventoryShadowMapper inventoryShadowMapper;

    @Override
    public List<String> listStoreSkuScope(String storeId, String erpStoreCode) {
        LinkedHashSet<String> skuCodes = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(storeId)) {
            List<StoreProductDO> storeProducts = storeProductMapper.selectListByStoreId(storeId);
            if (CollUtil.isNotEmpty(storeProducts)) {
                List<String> productSkuIds = storeProducts.stream()
                        .map(StoreProductDO::getProductSkuId)
                        .filter(StrUtil::isNotBlank)
                        .toList();
                if (CollUtil.isNotEmpty(productSkuIds)) {
                    List<SkuTableDO> skus = skuTableMapper.selectListByProductSkuIds(productSkuIds);
                    skus.stream()
                            .map(SkuTableDO::getProductSkuCode)
                            .filter(StrUtil::isNotBlank)
                            .forEach(skuCodes::add);
                }
            }
        }
        storeGoodsShadowMapper.selectActiveSkuCodesByErpStoreCode(erpStoreCode).forEach(skuCodes::add);
        inventoryShadowMapper.selectActiveSkuCodes(storeId, erpStoreCode).forEach(skuCodes::add);
        return skuCodes.stream().filter(StrUtil::isNotBlank).toList();
    }
}
