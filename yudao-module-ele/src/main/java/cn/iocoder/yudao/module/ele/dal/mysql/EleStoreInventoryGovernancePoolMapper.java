package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryGovernancePoolDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EleStoreInventoryGovernancePoolMapper extends BaseMapperX<EleStoreInventoryGovernancePoolDO> {

    default EleStoreInventoryGovernancePoolDO selectBySkuCodeKey(Long platformId, String merchantCode,
                                                                 String erpStoreCode, String skuCode, String reasonCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreInventoryGovernancePoolDO>()
                .eq(EleStoreInventoryGovernancePoolDO::getPlatformId, platformId)
                .eq(EleStoreInventoryGovernancePoolDO::getMerchantCode, merchantCode)
                .eq(EleStoreInventoryGovernancePoolDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreInventoryGovernancePoolDO::getSkuCode, skuCode)
                .eq(EleStoreInventoryGovernancePoolDO::getReasonCode, reasonCode));
    }

    default EleStoreInventoryGovernancePoolDO selectBySubSkuCodeKey(Long platformId, String merchantCode,
                                                                    String erpStoreCode, String subSkuCode, String reasonCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreInventoryGovernancePoolDO>()
                .eq(EleStoreInventoryGovernancePoolDO::getPlatformId, platformId)
                .eq(EleStoreInventoryGovernancePoolDO::getMerchantCode, merchantCode)
                .eq(EleStoreInventoryGovernancePoolDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreInventoryGovernancePoolDO::getSubSkuCode, subSkuCode)
                .eq(EleStoreInventoryGovernancePoolDO::getReasonCode, reasonCode));
    }
}
