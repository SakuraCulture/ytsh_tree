package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.List;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.*;

/**
 * 门店 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface StoreMapper extends BaseMapperX<StoreDO> {

    default PageResult<StoreDO> selectPage(StorePageReqVO reqVO) {
        LambdaQueryWrapperX<StoreDO> query = new LambdaQueryWrapperX<StoreDO>()
                .eqIfPresent(StoreDO::getRegionCode, reqVO.getRegionCode())
                .likeIfPresent(StoreDO::getAddress, reqVO.getAddress())
                .eqIfPresent(StoreDO::getArea, reqVO.getArea())
                .eqIfPresent(StoreDO::getStoreStatus, reqVO.getStoreStatus())
                .betweenIfPresent(StoreDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StoreDO::getStoreId);
        boolean hasStoreId = StringUtils.hasText(reqVO.getStoreId());
        boolean hasStoreName = StringUtils.hasText(reqVO.getStoreName());
        if (hasStoreId || hasStoreName) {
            query.and(w -> {
                if (hasStoreId) {
                    w.like(StoreDO::getStoreId, reqVO.getStoreId());
                }
                if (hasStoreName) {
                    if (hasStoreId) {
                        w.or();
                    }
                    w.like(StoreDO::getStoreName, reqVO.getStoreName());
                }
            });
        }
        return selectPage(reqVO, query);
    }

    default StoreDO selectByStoreName(String storeName) {
        return selectOne(StoreDO::getStoreName, storeName);
    }

    default List<StoreDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<StoreDO>()
                .orderByDesc(StoreDO::getStoreId));
    }

}
