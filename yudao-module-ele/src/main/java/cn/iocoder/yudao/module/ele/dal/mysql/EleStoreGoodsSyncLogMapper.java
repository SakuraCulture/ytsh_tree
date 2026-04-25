package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsSyncLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EleStoreGoodsSyncLogMapper extends BaseMapperX<EleStoreGoodsSyncLogDO> {

    default EleStoreGoodsSyncLogDO selectLatestByPlatformStoreId(String platformStoreId) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsSyncLogDO>()
                .eq(EleStoreGoodsSyncLogDO::getPlatformStoreId, platformStoreId)
                .orderByDesc(EleStoreGoodsSyncLogDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default PageResult<EleStoreGoodsSyncLogDO> selectPage(String platformStoreId, String erpStoreCode,
                                                          String skuCode, Boolean success,
                                                          PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<EleStoreGoodsSyncLogDO>()
                .likeIfPresent(EleStoreGoodsSyncLogDO::getPlatformStoreId, platformStoreId)
                .likeIfPresent(EleStoreGoodsSyncLogDO::getErpStoreCode, erpStoreCode)
                .likeIfPresent(EleStoreGoodsSyncLogDO::getSkuCode, skuCode)
                .eqIfPresent(EleStoreGoodsSyncLogDO::getSuccess, success)
                .orderByDesc(EleStoreGoodsSyncLogDO::getCreateTime));
    }
}
