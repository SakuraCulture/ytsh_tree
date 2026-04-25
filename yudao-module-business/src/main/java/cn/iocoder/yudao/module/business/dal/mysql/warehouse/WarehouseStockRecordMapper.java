package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordPageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStockRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface WarehouseStockRecordMapper extends BaseMapperX<WarehouseStockRecordDO> {

    default PageResult<WarehouseStockRecordDO> selectPage(WarehouseStockRecordPageReqVO reqVO, Collection<Long> productSkuIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarehouseStockRecordDO>()
                .eqIfPresent(WarehouseStockRecordDO::getStockRecordId, reqVO.getStockRecordId())
                .eqIfPresent(WarehouseStockRecordDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WarehouseStockRecordDO::getWarehouseProductId, reqVO.getWarehouseProductId())
                .inIfPresent(WarehouseStockRecordDO::getProductSkuId, productSkuIds)
                .eqIfPresent(WarehouseStockRecordDO::getBizType, reqVO.getBizType())
                .likeIfPresent(WarehouseStockRecordDO::getBizNo, reqVO.getBizNo())
                .betweenIfPresent(WarehouseStockRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseStockRecordDO::getStockRecordId));
    }

}
