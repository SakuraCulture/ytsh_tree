package cn.iocoder.yudao.module.business.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreStockDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店库存 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface StoreStockMapper extends BaseMapperX<StoreStockDO> {

    default StoreStockDO selectByStoreProductId(String storeProductId) {
        return selectOne(new LambdaQueryWrapperX<StoreStockDO>()
                .eq(StoreStockDO::getStoreProductId, storeProductId)
                .last("LIMIT 1"));
    }

}