package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductPageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseProductDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseProductMapper extends BaseMapperX<WarehouseProductDO> {

    default PageResult<WarehouseProductDO> selectPage(WarehouseProductPageReqVO reqVO, List<Long> productSkuIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarehouseProductDO>()
                .eqIfPresent(WarehouseProductDO::getWarehouseProductId, reqVO.getWarehouseProductId())
                .eqIfPresent(WarehouseProductDO::getWarehouseId, reqVO.getWarehouseId())
                .inIfPresent(WarehouseProductDO::getProductSkuId, productSkuIds)
                .likeIfPresent(WarehouseProductDO::getWarehouseProductLocation, reqVO.getWarehouseProductLocation())
                .betweenIfPresent(WarehouseProductDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseProductDO::getWarehouseProductId));
    }

    default WarehouseProductDO selectByWarehouseIdAndProductSkuId(String warehouseId, Long productSkuId) {
        return selectOne(new LambdaQueryWrapperX<WarehouseProductDO>()
                .eq(WarehouseProductDO::getWarehouseId, warehouseId)
                .eq(WarehouseProductDO::getProductSkuId, productSkuId));
    }

    default List<WarehouseProductDO> selectListByWarehouseId(String warehouseId) {
        return selectList(new LambdaQueryWrapperX<WarehouseProductDO>()
                .eq(WarehouseProductDO::getWarehouseId, warehouseId)
                .orderByDesc(WarehouseProductDO::getWarehouseProductId));
    }

    default List<WarehouseProductDO> selectListByWarehouseIds(Collection<String> warehouseIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseProductDO>()
                .inIfPresent(WarehouseProductDO::getWarehouseId, warehouseIds)
                .orderByDesc(WarehouseProductDO::getWarehouseProductId));
    }

    default List<WarehouseProductDO> selectListByWarehouseProductIds(Collection<Long> warehouseProductIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseProductDO>()
                .inIfPresent(WarehouseProductDO::getWarehouseProductId, warehouseProductIds)
                .orderByDesc(WarehouseProductDO::getWarehouseProductId));
    }

}
