package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapperX<OrderDO> {

    default boolean insertOrUpdate(OrderDO order) {
        List<OrderDO> existing = selectList(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getOrderId, order.getOrderId())
                .eq(OrderDO::getDeleted, false)
                .last("LIMIT 1"));
        if (existing != null && !existing.isEmpty()) {
            update(order, new LambdaQueryWrapperX<OrderDO>()
                    .eq(OrderDO::getOrderId, order.getOrderId()));
            return false;
        } else {
            insert(order);
            return true;
        }
    }

    default List<OrderDO> selectListByStoreCodeAndTime(
            String storeCode,
            Integer status,
            Long startTime,
            Long endTime) {
        return selectList(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getStoreCode, storeCode)
                .eq(status != null, OrderDO::getOrderStatus, status)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .eq(OrderDO::getDeleted, false)
                .orderByDesc(OrderDO::getCreateTime));
    }

    default Long countByStoreCodeAndTime(
            String storeCode,
            Integer status,
            Long startTime,
            Long endTime) {
        return selectCount(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getStoreCode, storeCode)
                .eq(status != null, OrderDO::getOrderStatus, status)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .eq(OrderDO::getDeleted, false));
    }

    default List<OrderDO> selectByOrderIds(@Param("orderIds") Collection<String> orderIds) {
        return selectList(new LambdaQueryWrapperX<OrderDO>()
                .in(OrderDO::getOrderId, orderIds)
                .eq(OrderDO::getDeleted, false));
    }

    /**
     * 批量查询已存在的订单ID（用于批量插入前判断）
     * @param orderIds 订单ID列表
     * @return 已存在的订单ID列表
     */
    default List<String> selectExistingOrderIds(Collection<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        List<OrderDO> orders = selectList(new LambdaQueryWrapperX<OrderDO>()
                .select(OrderDO::getOrderId)
                .in(OrderDO::getOrderId, orderIds)
                .eq(OrderDO::getDeleted, false));
        return orders.stream()
                .map(OrderDO::getOrderId)
                .collect(java.util.stream.Collectors.toList());
    }

    default Long selectCountByPlatformStoreId(String platformStoreId) {
        return selectCount(new LambdaQueryWrapperX<OrderDO>()
                .eq(OrderDO::getStoreCode, platformStoreId));
    }
}
