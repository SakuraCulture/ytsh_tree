package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockPageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStockDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseStockMapper extends BaseMapperX<WarehouseStockDO> {

    default PageResult<WarehouseStockDO> selectPage(WarehouseStockPageReqVO reqVO, Collection<Long> warehouseProductIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarehouseStockDO>()
                .eqIfPresent(WarehouseStockDO::getWarehouseStockId, reqVO.getWarehouseStockId())
                .eqIfPresent(WarehouseStockDO::getWarehouseProductId, reqVO.getWarehouseProductId())
                .inIfPresent(WarehouseStockDO::getWarehouseProductId, warehouseProductIds)
                .betweenIfPresent(WarehouseStockDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseStockDO::getWarehouseStockId));
    }

    default WarehouseStockDO selectByWarehouseProductId(Long warehouseProductId) {
        return selectOne(WarehouseStockDO::getWarehouseProductId, warehouseProductId);
    }

    default List<WarehouseStockDO> selectListByWarehouseProductIds(Collection<Long> warehouseProductIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseStockDO>()
                .inIfPresent(WarehouseStockDO::getWarehouseProductId, warehouseProductIds)
                .orderByDesc(WarehouseStockDO::getWarehouseStockId));
    }

}
