package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLinePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseLineMapper extends BaseMapperX<WarehouseLineDO> {

    default PageResult<WarehouseLineDO> selectPageRaw(WarehouseLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarehouseLineDO>()
                .eqIfPresent(WarehouseLineDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(WarehouseLineDO::getLineCode, reqVO.getLineCode())
                .likeIfPresent(WarehouseLineDO::getLineName, reqVO.getLineName())
                .eqIfPresent(WarehouseLineDO::getLineStatus, reqVO.getLineStatus())
                .betweenIfPresent(WarehouseLineDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseLineDO::getLineId));
    }

    default WarehouseLineDO selectByWarehouseIdAndLineCode(String warehouseId, String lineCode) {
        return selectOne(new LambdaQueryWrapperX<WarehouseLineDO>()
                .eq(WarehouseLineDO::getWarehouseId, warehouseId)
                .eq(WarehouseLineDO::getLineCode, lineCode));
    }

    default List<WarehouseLineDO> selectActiveListByWarehouseId(String warehouseId) {
        return selectList(new LambdaQueryWrapperX<WarehouseLineDO>()
                .eq(WarehouseLineDO::getWarehouseId, warehouseId)
                .eq(WarehouseLineDO::getLineStatus, 1)
                .orderByAsc(WarehouseLineDO::getLineCode));
    }

    default List<WarehouseLineDO> selectListByLineIds(Collection<Long> lineIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseLineDO>()
                .inIfPresent(WarehouseLineDO::getLineId, lineIds)
                .orderByAsc(WarehouseLineDO::getLineCode));
    }
}
