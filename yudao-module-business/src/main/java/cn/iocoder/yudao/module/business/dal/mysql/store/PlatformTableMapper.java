package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.*;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformTableDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlatformTableMapper extends BaseMapperX<PlatformTableDO> {

    default List<PlatformTableDO> selectListByStoreId(String storeId) {
        return selectList(PlatformTableDO::getStoreId, storeId);
    }

    default List<PlatformTableDO> selectListByPlatformIdAndPlatformStoreId(Long platformId, String platformStoreId) {
        return selectList(new LambdaQueryWrapperX<PlatformTableDO>()
                .eq(PlatformTableDO::getPlatformId, platformId)
                .eq(PlatformTableDO::getPlatformStoreId, platformStoreId)
                .eq(PlatformTableDO::getStatus, 1)
                .orderByAsc(PlatformTableDO::getStoreId));
    }

    default List<PlatformTableDO> selectEnabledListByPlatformIdAndStoreIds(Long platformId, List<String> storeIds) {
        return selectList(new LambdaQueryWrapperX<PlatformTableDO>()
                .in(PlatformTableDO::getStoreId, storeIds)
                .eq(PlatformTableDO::getPlatformId, platformId)
                .eq(PlatformTableDO::getStatus, 1)
                .isNotNull(PlatformTableDO::getPlatformStoreId)
                .orderByAsc(PlatformTableDO::getStoreId));
    }

    default List<PlatformTableDO> selectEnabledListByPlatformIdAndPlatformStoreIds(Long platformId, Collection<String> platformStoreIds) {
        return selectList(new LambdaQueryWrapperX<PlatformTableDO>()
                .eq(PlatformTableDO::getPlatformId, platformId)
                .inIfPresent(PlatformTableDO::getPlatformStoreId, platformStoreIds)
                .eq(PlatformTableDO::getStatus, 1)
                .isNotNull(PlatformTableDO::getPlatformStoreId)
                .orderByAsc(PlatformTableDO::getStoreId));
    }

    default List<PlatformTableDO> selectListByStoreIds(Collection<String> storeIds) {
        return selectList(new LambdaQueryWrapperX<PlatformTableDO>()
                .in(PlatformTableDO::getStoreId, storeIds)
                .eq(PlatformTableDO::getDeleted, false)
                .orderByAsc(PlatformTableDO::getStoreId));
    }

}
