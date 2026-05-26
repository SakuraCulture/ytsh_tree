package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
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

    @InterceptorIgnore(dataPermission = "true")
    @Update("INSERT INTO order_table (order_id, order_status, create_time, pay_time, buyer_name, buyer_phone, " +
            "buyer_address, delivery_name, delivery_phone, delivery_status, delivery_fee, " +
            "total_fee, pay_fee, discount_fee, post_fee, package_fee, " +
            "channel_source_id, channel_source_name, channel_order_id, store_code, longitude, latitude, " +
            "user_id, arrive_type, remark, store_id, order_from, order_index, estimated_income, " +
            "sub_orders_json, discounts_json, etl_time, tenant_id, " +
            "creator, update_time, deleted) " +
            "VALUES (#{orderId}, #{orderStatus}, #{createTime}, #{payTime}, #{buyerName}, #{buyerPhone}, " +
            "#{buyerAddress}, #{deliveryName}, #{deliveryPhone}, #{deliveryStatus}, #{deliveryFee}, " +
            "#{totalFee}, #{payFee}, #{discountFee}, #{postFee}, #{packageFee}, " +
            "#{channelSourceId}, #{channelSourceName}, #{channelOrderId}, #{storeCode}, #{longitude}, #{latitude}, " +
            "#{userId}, #{arriveType}, #{remark}, #{storeId}, #{orderFrom}, #{orderIndex}, #{estimatedIncome}, " +
            "#{subOrdersJson}, #{discountsJson}, #{etlTime}, #{tenantId}, " +
            "#{creator}, #{updateTime}, #{deleted}) " +
            "ON DUPLICATE KEY UPDATE " +
            "order_status = VALUES(order_status), " +
            "pay_time = VALUES(pay_time), " +
            "buyer_name = VALUES(buyer_name), " +
            "buyer_phone = VALUES(buyer_phone), " +
            "buyer_address = VALUES(buyer_address), " +
            "delivery_name = VALUES(delivery_name), " +
            "delivery_phone = VALUES(delivery_phone), " +
            "delivery_status = VALUES(delivery_status), " +
            "delivery_fee = VALUES(delivery_fee), " +
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
            "user_id = VALUES(user_id), " +
            "arrive_type = VALUES(arrive_type), " +
            "remark = VALUES(remark), " +
            "store_id = VALUES(store_id), " +
            "order_from = VALUES(order_from), " +
            "order_index = VALUES(order_index), " +
            "estimated_income = VALUES(estimated_income), " +
            "sub_orders_json = VALUES(sub_orders_json), " +
            "discounts_json = VALUES(discounts_json), " +
            "etl_time = VALUES(etl_time), " +
            "tenant_id = VALUES(tenant_id), " +
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

    
    default Map<Integer, Long> selectCountGroupByStatus(
            String platformStoreId,
            Long startTime,
            Long endTime,
            String orderId,
            String channelOrderId,
            String buyerName,
            String buyerPhoneSuffix,
            List<String> orderIdsBySkuName,
            String channelType,
            Integer arriveType,
            String exceptionType,
            Integer deliveryMode,
            List<String> orderIdsByPlatform,
            String address) {
        return selectList(new LambdaQueryWrapperX<OrderDO>()
                .select(OrderDO::getOrderStatus)
                .eq(platformStoreId != null, OrderDO::getStoreCode, platformStoreId)
                .ge(startTime != null, OrderDO::getCreateTime, startTime)
                .le(endTime != null, OrderDO::getCreateTime, endTime)
                .like(orderId != null, OrderDO::getOrderId, orderId)
                .like(channelOrderId != null, OrderDO::getChannelOrderId, channelOrderId)
                .like(buyerName != null, OrderDO::getBuyerName, buyerName)
                .like(buyerPhoneSuffix != null, OrderDO::getBuyerPhone, buyerPhoneSuffix)
                .like(StrUtil.isNotBlank(address), OrderDO::getBuyerAddress, address)
                .in(orderIdsBySkuName != null && !orderIdsBySkuName.isEmpty(), OrderDO::getOrderId, orderIdsBySkuName)
                .eq(arriveType != null, OrderDO::getArriveType, arriveType)
                .eq("exception".equals(exceptionType), OrderDO::getOrderStatus, -2)
                .ne("normal".equals(exceptionType), OrderDO::getOrderStatus, -2)
                .in(orderIdsByPlatform != null && !orderIdsByPlatform.isEmpty(), OrderDO::getOrderId, orderIdsByPlatform)
                .eq(OrderDO::getDeleted, false))
                .stream()
                .filter(order -> order.getOrderStatus() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        OrderDO::getOrderStatus,
                        java.util.stream.Collectors.counting()));
    }

    @Update("UPDATE order_table SET settlement_amount = #{amount}, " +
            "settlement_status = #{status}, updater = #{updater}, update_time = #{updateTime} " +
            "WHERE order_id = #{orderId} AND deleted = 0")
    void updateSettlementInfo(@Param("orderId") String orderId,
                              @Param("amount") BigDecimal amount,
                              @Param("status") Integer status,
                              @Param("updater") String updater,
                              @Param("updateTime") Long updateTime);

    @Update("UPDATE order_table SET " +
            "settlement_amount = #{amount}, " +
            "settlement_status = #{status}, " +
            "last_bill_date = #{billDate}, " +
            "last_bill_amount = #{billAmount}, " +
            "last_bill_status = #{billStatus}, " +
            "updater = #{updater}, " +
            "update_time = #{updateTime} " +
            "WHERE order_id = #{orderId} AND deleted = 0")
    void updateSettlementWithBillInfo(@Param("orderId") String orderId,
                                       @Param("amount") BigDecimal amount,
                                       @Param("status") Integer status,
                                       @Param("billDate") java.time.LocalDate billDate,
                                       @Param("billAmount") Long billAmount,
                                       @Param("billStatus") Integer billStatus,
                                       @Param("updater") String updater,
                                       @Param("updateTime") Long updateTime);
}
