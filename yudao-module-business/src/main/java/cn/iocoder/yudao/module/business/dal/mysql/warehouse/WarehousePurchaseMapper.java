package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchasePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehousePurchaseDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface WarehousePurchaseMapper extends BaseMapperX<WarehousePurchaseDO> {

    default PageResult<WarehousePurchaseDO> selectPage(WarehousePurchasePageReqVO reqVO, Collection<Long> purchaseOrderIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarehousePurchaseDO>()
                .eqIfPresent(WarehousePurchaseDO::getPurchaseOrderId, reqVO.getPurchaseOrderId())
                .inIfPresent(WarehousePurchaseDO::getPurchaseOrderId, purchaseOrderIds)
                .likeIfPresent(WarehousePurchaseDO::getPurchaseOrderNo, reqVO.getPurchaseOrderNo())
                .eqIfPresent(WarehousePurchaseDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(WarehousePurchaseDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WarehousePurchaseDO::getOrderStatus, reqVO.getOrderStatus())
                .eqIfPresent(WarehousePurchaseDO::getReceiveStatus, reqVO.getReceiveStatus())
                .likeIfPresent(WarehousePurchaseDO::getPurchaser, reqVO.getPurchaser())
                .betweenIfPresent(WarehousePurchaseDO::getPurchaseDate, reqVO.getPurchaseDate())
                .betweenIfPresent(WarehousePurchaseDO::getAuditDate, reqVO.getAuditDate())
                .betweenIfPresent(WarehousePurchaseDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehousePurchaseDO::getPurchaseOrderId));
    }

    default int updateByIdAndOrderStatus(Long purchaseOrderId, String orderStatus, WarehousePurchaseDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<WarehousePurchaseDO>()
                .eq(WarehousePurchaseDO::getPurchaseOrderId, purchaseOrderId)
                .eq(WarehousePurchaseDO::getOrderStatus, orderStatus));
    }

    default WarehousePurchaseDO selectByPurchaseOrderNo(String purchaseOrderNo) {
        return selectOne(WarehousePurchaseDO::getPurchaseOrderNo, purchaseOrderNo);
    }

}
