package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderBillDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderBillMapper extends BaseMapperX<OrderBillDO> {

    @Insert("INSERT INTO order_bill_table " +
            "(bill_id, order_id, order_date, refund_id, merchant_code, store_code, shop_id, store_name, " +
            "channel_type, bill_date, status, bill_amount, item_price, package_fee, delivery_fee, " +
            "shop_marketing_fee, platform_fee, donation_fee, user_pay_shipping_amount, " +
            "user_online_pay_amount, product_preferences, not_product_preferences, " +
            "performance_service_fee, platform_charge_fee, activity_amount, " +
            "bill_type_desc, shipping_type, settle_order_id, sync_time, create_time, update_time, tenant_id, deleted) " +
            "VALUES " +
            "(#{billId}, #{orderId}, #{orderDate}, #{refundId}, #{merchantCode}, #{storeCode}, #{shopId}, #{storeName}, " +
            "#{channelType}, #{billDate}, #{status}, #{billAmount}, #{itemPrice}, #{packageFee}, #{deliveryFee}, " +
            "#{shopMarketingFee}, #{platformFee}, #{donationFee}, #{userPayShippingAmount}, " +
            "#{userOnlinePayAmount}, #{productPreferences}, #{notProductPreferences}, " +
            "#{performanceServiceFee}, #{platformChargeFee}, #{activityAmount}, " +
            "#{billTypeDesc}, #{shippingType}, #{settleOrderId}, #{syncTime}, #{createTime}, #{updateTime}, #{tenantId}, #{deleted}) " +
            "ON DUPLICATE KEY UPDATE " +
            "order_date=VALUES(order_date), refund_id=VALUES(refund_id), " +
            "merchant_code=VALUES(merchant_code), store_code=VALUES(store_code), shop_id=VALUES(shop_id), " +
            "store_name=VALUES(store_name), channel_type=VALUES(channel_type), bill_date=VALUES(bill_date), " +
            "status=VALUES(status), bill_amount=VALUES(bill_amount), " +
            "item_price=VALUES(item_price), package_fee=VALUES(package_fee), " +
            "delivery_fee=VALUES(delivery_fee), shop_marketing_fee=VALUES(shop_marketing_fee), " +
            "platform_fee=VALUES(platform_fee), donation_fee=VALUES(donation_fee), " +
            "user_pay_shipping_amount=VALUES(user_pay_shipping_amount), " +
            "user_online_pay_amount=VALUES(user_online_pay_amount), " +
            "product_preferences=VALUES(product_preferences), not_product_preferences=VALUES(not_product_preferences), " +
            "performance_service_fee=VALUES(performance_service_fee), " +
            "platform_charge_fee=VALUES(platform_charge_fee), activity_amount=VALUES(activity_amount), " +
            "bill_type_desc=VALUES(bill_type_desc), shipping_type=VALUES(shipping_type), " +
            "settle_order_id=VALUES(settle_order_id), sync_time=VALUES(sync_time), update_time=VALUES(update_time)")
    int rawInsertOrUpdate(OrderBillDO billDO);

    @Select("SELECT * FROM order_bill_table WHERE order_id = #{orderId} AND deleted = 0 ORDER BY bill_date DESC LIMIT 1")
    OrderBillDO selectLatestByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM order_bill_table WHERE order_id = #{orderId} AND deleted = 0 ORDER BY bill_date DESC")
    List<OrderBillDO> selectAllByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM order_bill_table WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time ASC")
    List<OrderBillDO> selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT COALESCE(SUM(bill_amount), 0) FROM order_bill_table " +
            "WHERE order_id = #{orderId} AND deleted = 0 " +
            "AND bill_type_desc IN ('正向单', '代运营业务')")
    Long sumSettlementAmountByOrderId(@Param("orderId") String orderId);

    @Select("SELECT COALESCE(SUM(bill_amount), 0) FROM order_bill_table " +
            "WHERE order_id = #{orderId} AND deleted = 0 AND bill_type_desc = '正向单'")
    Long sumPositiveAmountByOrderId(@Param("orderId") String orderId);

    @Select("SELECT COALESCE(SUM(bill_amount), 0) FROM order_bill_table " +
            "WHERE order_id = #{orderId} AND deleted = 0 AND bill_type_desc = '代运营业务'")
    Long sumOperationAmountByOrderId(@Param("orderId") String orderId);

    @Select("SELECT COUNT(DISTINCT settle_order_id) FROM order_bill_table " +
            "WHERE order_id = #{orderId} AND deleted = 0 " +
            "AND bill_type_desc IN ('正向单', '代运营业务')")
    Integer countDistinctSettleOrders(@Param("orderId") String orderId);

    @Select("SELECT settle_order_id, order_id, " +
            "COALESCE(SUM(bill_amount), 0) AS total_amount, " +
            "COALESCE(SUM(CASE WHEN bill_type_desc = '正向单' THEN bill_amount ELSE 0 END), 0) AS positive_amount, " +
            "COALESCE(SUM(CASE WHEN bill_type_desc = '代运营业务' THEN bill_amount ELSE 0 END), 0) AS operation_amount, " +
            "COUNT(*) AS bill_count " +
            "FROM order_bill_table " +
            "WHERE settle_order_id = #{settleOrderId} AND order_id = #{orderId} AND deleted = 0 " +
            "AND bill_type_desc IN ('正向单', '代运营业务') " +
            "GROUP BY settle_order_id, order_id")
    cn.iocoder.yudao.module.ele.service.dto.BillSettlementSummary summarizeBySettleOrder(
            @Param("settleOrderId") String settleOrderId,
            @Param("orderId") String orderId);
}
