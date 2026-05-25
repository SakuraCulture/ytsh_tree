package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDiscountDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OrderDiscountMapper extends BaseMapperX<OrderDiscountDO> {

    default List<OrderDiscountDO> selectByOrderIds(@Param("orderIds") Collection<String> orderIds) {
        return selectList(new LambdaQueryWrapperX<OrderDiscountDO>()
                .in(OrderDiscountDO::getOrderId, orderIds)
                .eq(OrderDiscountDO::getDeleted, false));
    }

    /**
     * UPSERT 批量插入/更新
     * 基于 (order_id, activity_id) 唯一索引，存在则 UPDATE，不存在则 INSERT
     */
    @Insert("<script>" +
            "INSERT INTO order_discount_table (order_id, sub_order_id, activity_id, activity_name, " +
            "activity_order_type, discount_type, discount_fee, merchant_fee, platform_fee, " +
            "creator, create_time, updater, update_time, deleted) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.orderId}, #{item.subOrderId}, #{item.activityId}, #{item.activityName}, " +
            "#{item.activityOrderType}, #{item.discountType}, #{item.discountFee}, " +
            "#{item.merchantFee}, #{item.platformFee}, " +
            "#{item.creator}, #{item.createTime}, #{item.updater}, #{item.updateTime}, #{item.deleted})" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "sub_order_id = VALUES(sub_order_id), activity_name = VALUES(activity_name), " +
            "activity_order_type = VALUES(activity_order_type), " +
            "discount_type = VALUES(discount_type), discount_fee = VALUES(discount_fee), " +
            "merchant_fee = VALUES(merchant_fee), platform_fee = VALUES(platform_fee), " +
            "updater = VALUES(updater), update_time = VALUES(update_time), " +
            "deleted = VALUES(deleted)" +
            "</script>")
    int upsertBatch(@Param("items") List<OrderDiscountDO> items);
}
