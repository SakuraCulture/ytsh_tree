package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface EleStoreGoodsShadowMapper extends BaseMapperX<EleStoreGoodsShadowDO> {

    default EleStoreGoodsShadowDO selectByBizKey(Long platformId, String merchantCode, String erpStoreCode, String skuCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .eq(EleStoreGoodsShadowDO::getPlatformId, platformId)
                .eq(EleStoreGoodsShadowDO::getMerchantCode, merchantCode)
                .eq(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreGoodsShadowDO::getSkuCode, skuCode));
    }

    default List<EleStoreGoodsShadowDO> selectActiveList(Collection<String> matchStatuses, String storeId,
                                                         String erpStoreCode, String skuCode, String title) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .inIfPresent(EleStoreGoodsShadowDO::getMatchStatus, matchStatuses)
                .eqIfPresent(EleStoreGoodsShadowDO::getStoreId, storeId)
                .eqIfPresent(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .likeIfPresent(EleStoreGoodsShadowDO::getSkuCode, skuCode)
                .likeIfPresent(EleStoreGoodsShadowDO::getTitle, title)
                .orderByDesc(EleStoreGoodsShadowDO::getUpdateTime));
    }
}
