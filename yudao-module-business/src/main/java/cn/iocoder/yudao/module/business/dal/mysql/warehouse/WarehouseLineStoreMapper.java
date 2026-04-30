package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineStoreDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseLineStoreMapper extends BaseMapperX<WarehouseLineStoreDO> {

    default List<WarehouseLineStoreDO> selectListByLineId(Long lineId) {
        return selectList(new LambdaQueryWrapperX<WarehouseLineStoreDO>()
                .eq(WarehouseLineStoreDO::getLineId, lineId)
                .orderByAsc(WarehouseLineStoreDO::getSortNo)
                .orderByAsc(WarehouseLineStoreDO::getId));
    }

    default List<WarehouseLineStoreDO> selectListByLineIds(Collection<Long> lineIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseLineStoreDO>()
                .inIfPresent(WarehouseLineStoreDO::getLineId, lineIds)
                .orderByAsc(WarehouseLineStoreDO::getSortNo)
                .orderByAsc(WarehouseLineStoreDO::getId));
    }

    default void deleteByLineId(Long lineId) {
        delete(new LambdaQueryWrapperX<WarehouseLineStoreDO>()
                .eq(WarehouseLineStoreDO::getLineId, lineId));
    }
}
