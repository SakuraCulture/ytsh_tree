package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseMapper extends BaseMapperX<WarehouseDO> {

    default PageResult<WarehouseDO> selectPage(WarehousePageReqVO reqVO) {
        LambdaQueryWrapperX<WarehouseDO> query = new LambdaQueryWrapperX<WarehouseDO>()
                .eqIfPresent(WarehouseDO::getWarehouseType, reqVO.getWarehouseType())
                .eqIfPresent(WarehouseDO::getRegionCode, reqVO.getRegionCode())
                .likeIfPresent(WarehouseDO::getAddress, reqVO.getAddress())
                .eqIfPresent(WarehouseDO::getWarehouseStatus, reqVO.getWarehouseStatus())
                .eqIfPresent(WarehouseDO::getIsDefault, reqVO.getIsDefault())
                .betweenIfPresent(WarehouseDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseDO::getWarehouseId);
        boolean hasWarehouseId = StringUtils.hasText(reqVO.getWarehouseId());
        boolean hasWarehouseCode = StringUtils.hasText(reqVO.getWarehouseCode());
        boolean hasWarehouseName = StringUtils.hasText(reqVO.getWarehouseName());
        if (hasWarehouseId || hasWarehouseCode || hasWarehouseName) {
            query.and(w -> {
                if (hasWarehouseId) {
                    w.like(WarehouseDO::getWarehouseId, reqVO.getWarehouseId());
                }
                if (hasWarehouseCode) {
                    if (hasWarehouseId) {
                        w.or();
                    }
                    w.like(WarehouseDO::getWarehouseCode, reqVO.getWarehouseCode());
                }
                if (hasWarehouseName) {
                    if (hasWarehouseId || hasWarehouseCode) {
                        w.or();
                    }
                    w.like(WarehouseDO::getWarehouseName, reqVO.getWarehouseName());
                }
            });
        }
        return selectPage(reqVO, query);
    }

    default WarehouseDO selectByWarehouseCode(String warehouseCode) {
        return selectOne(WarehouseDO::getWarehouseCode, warehouseCode);
    }

    default WarehouseDO selectByWarehouseName(String warehouseName) {
        return selectOne(WarehouseDO::getWarehouseName, warehouseName);
    }

    default WarehouseDO selectByDefaultWarehouse() {
        return selectOne(WarehouseDO::getIsDefault, 1);
    }

    default List<WarehouseDO> selectListByWarehouseStatus(Integer warehouseStatus) {
        return selectList(new LambdaQueryWrapperX<WarehouseDO>()
                .eqIfPresent(WarehouseDO::getWarehouseStatus, warehouseStatus)
                .orderByDesc(WarehouseDO::getWarehouseId));
    }

    default List<WarehouseDO> selectListByWarehouseIds(Collection<String> warehouseIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseDO>()
                .inIfPresent(WarehouseDO::getWarehouseId, warehouseIds)
                .orderByDesc(WarehouseDO::getWarehouseId));
    }

}
