package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyPageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStoreSupplyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseStoreSupplyMapper extends BaseMapperX<WarehouseStoreSupplyDO> {

    default PageResult<WarehouseStoreSupplyDO> selectPageRaw(WarehouseStoreSupplyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarehouseStoreSupplyDO>()
                .eqIfPresent(WarehouseStoreSupplyDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WarehouseStoreSupplyDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(WarehouseStoreSupplyDO::getSupplyStatus, reqVO.getSupplyStatus())
                .eqIfPresent(WarehouseStoreSupplyDO::getIsPrimary, reqVO.getIsPrimary())
                .betweenIfPresent(WarehouseStoreSupplyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseStoreSupplyDO::getId));
    }

    default WarehouseStoreSupplyDO selectByWarehouseIdAndStoreId(String warehouseId, String storeId) {
        return selectOne(new LambdaQueryWrapperX<WarehouseStoreSupplyDO>()
                .eq(WarehouseStoreSupplyDO::getWarehouseId, warehouseId)
                .eq(WarehouseStoreSupplyDO::getStoreId, storeId));
    }

    default List<WarehouseStoreSupplyDO> selectListByStoreId(String storeId) {
        return selectList(new LambdaQueryWrapperX<WarehouseStoreSupplyDO>()
                .eq(WarehouseStoreSupplyDO::getStoreId, storeId)
                .orderByDesc(WarehouseStoreSupplyDO::getIsPrimary)
                .orderByAsc(WarehouseStoreSupplyDO::getWarehouseId));
    }

    default List<WarehouseStoreSupplyDO> selectListByWarehouseId(String warehouseId) {
        return selectList(new LambdaQueryWrapperX<WarehouseStoreSupplyDO>()
                .eq(WarehouseStoreSupplyDO::getWarehouseId, warehouseId)
                .orderByDesc(WarehouseStoreSupplyDO::getIsPrimary)
                .orderByAsc(WarehouseStoreSupplyDO::getStoreId));
    }

    default List<WarehouseStoreSupplyDO> selectListByStoreIds(Collection<String> storeIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseStoreSupplyDO>()
                .inIfPresent(WarehouseStoreSupplyDO::getStoreId, storeIds)
                .orderByDesc(WarehouseStoreSupplyDO::getIsPrimary)
                .orderByAsc(WarehouseStoreSupplyDO::getWarehouseId));
    }
}
