package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Update("INSERT INTO order_table (order_id, order_status, create_time, pay_time, buyer_name, buyer_phone, " +
            "buyer_address, delivery_status, total_fee, pay_fee, discount_fee, post_fee, package_fee, " +
            "channel_source_id, channel_source_name, channel_order_id, store_code, longitude, latitude, remark, " +
            "creator, update_time, deleted) " +
            "VALUES (#{orderId}, #{orderStatus}, #{createTime}, #{payTime}, #{buyerName}, #{buyerPhone}, " +
            "#{buyerAddress}, #{deliveryStatus}, #{totalFee}, #{payFee}, #{discountFee}, #{postFee}, #{packageFee}, " +
            "#{channelSourceId}, #{channelSourceName}, #{channelOrderId}, #{storeCode}, #{longitude}, #{latitude}, " +
            "#{remark}, #{creator}, #{updateTime}, #{deleted}) " +
            "ON DUPLICATE KEY UPDATE " +
            "order_status = VALUES(order_status), " +
            "pay_time = VALUES(pay_time), " +
            "buyer_name = VALUES(buyer_name), " +
            "buyer_phone = VALUES(buyer_phone), " +
            "buyer_address = VALUES(buyer_address), " +
            "delivery_status = VALUES(delivery_status), " +
            "total_fee = VALUES(total_fee), " +
            "pay_fee = VALUES(pay_fee), " +
            "discount_fee = VALUES(discount_fee), " +
            "post_fee = VALUES(post_fee), " +
            "package_fee = VALUES(package_fee), " +
            "channel_source_id = VALUES(channel_source_id), " +
            "channel_source_name = VALUES(channel_source_name), " +
            "channel_order_id = VALUES(channel_order_id), " +
            "store_code = VALUES(store_code), " +
            "longitude = VALUES(longitude), " +
            "latitude = VALUES(latitude), " +
            "remark = VALUES(remark), " +
            "updater = VALUES(creator), " +
            "update_time = VALUES(update_time)")
    int upsertOrder(OrderDO order);

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

    /**
     * 按订单状态分组统计
     * @param platformStoreId 平台门店ID（可选）
     * @param startTime 开始时间（秒级时间戳）
     * @param endTime 结束时间（秒级时间戳）
     * @return 状态码为key，数量为value的Map
     */
    default Map<Integer, Long> selectCountGroupByStatus(
            String platformStoreId,
            Long startTime,
            Long endTime) {
        return selectList(new LambdaQueryWrapperX<OrderDO>()
                .select(OrderDO::getOrderStatus)
                .eq(platformStoreId != null, OrderDO::getStoreCode, platformStoreId)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .eq(OrderDO::getDeleted, false))
                .stream()
                .filter(order -> order.getOrderStatus() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        OrderDO::getOrderStatus,
                        java.util.stream.Collectors.counting()));
    }
}
