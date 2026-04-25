package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehousePurchaseDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface WarehousePurchaseDetailMapper extends BaseMapperX<WarehousePurchaseDetailDO> {

    default List<WarehousePurchaseDetailDO> selectListByPurchaseOrderId(Long purchaseOrderId) {
        return selectList(new LambdaQueryWrapperX<WarehousePurchaseDetailDO>()
                .eq(WarehousePurchaseDetailDO::getPurchaseOrderId, purchaseOrderId)
                .orderByAsc(WarehousePurchaseDetailDO::getDetailId));
    }

    default List<WarehousePurchaseDetailDO> selectListByPurchaseOrderIds(Collection<Long> purchaseOrderIds) {
        if (purchaseOrderIds == null || purchaseOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<WarehousePurchaseDetailDO>()
                .in(WarehousePurchaseDetailDO::getPurchaseOrderId, purchaseOrderIds)
                .orderByAsc(WarehousePurchaseDetailDO::getDetailId));
    }

    default void deleteByPurchaseOrderId(Long purchaseOrderId) {
        delete(new LambdaQueryWrapperX<WarehousePurchaseDetailDO>()
                .eq(WarehousePurchaseDetailDO::getPurchaseOrderId, purchaseOrderId));
    }

}
