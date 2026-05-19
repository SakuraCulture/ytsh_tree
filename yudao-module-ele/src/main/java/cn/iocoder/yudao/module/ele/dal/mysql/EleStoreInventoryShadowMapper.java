package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EleStoreInventoryShadowMapper extends BaseMapperX<EleStoreInventoryShadowDO> {

    default EleStoreInventoryShadowDO selectBySkuCodeKey(Long platformId, String merchantCode,
                                                          String erpStoreCode, String skuCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreInventoryShadowDO>()
                .eq(EleStoreInventoryShadowDO::getPlatformId, platformId)
                .eq(EleStoreInventoryShadowDO::getMerchantCode, merchantCode)
                .eq(EleStoreInventoryShadowDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreInventoryShadowDO::getSkuCode, skuCode));
    }

    default EleStoreInventoryShadowDO selectBySubSkuCodeKey(Long platformId, String merchantCode,
                                                             String erpStoreCode, String subSkuCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreInventoryShadowDO>()
                .eq(EleStoreInventoryShadowDO::getPlatformId, platformId)
                .eq(EleStoreInventoryShadowDO::getMerchantCode, merchantCode)
                .eq(EleStoreInventoryShadowDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreInventoryShadowDO::getSubSkuCode, subSkuCode));
    }
}
