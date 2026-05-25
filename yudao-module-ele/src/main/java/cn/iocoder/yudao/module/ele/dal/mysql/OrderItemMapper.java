package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderItemDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapperX<OrderItemDO> {

    default List<OrderItemDO> selectByOrderIds(@Param("orderIds") Collection<String> orderIds) {
        return selectList(new LambdaQueryWrapperX<OrderItemDO>()
                .in(OrderItemDO::getOrderId, orderIds)
                .eq(OrderItemDO::getDeleted, false));
    }

    /**
     * UPSERT 批量插入/更新
     * 基于 subOrderId 唯一索引，存在则 UPDATE，不存在则 INSERT
     */
    @Insert("<script>" +
            "INSERT INTO order_item_table (sub_order_id, order_id, sku_code, sub_sku_code, product_sku_id, " +
            "sku_name, barcode, specification, weight, total_weight, buy_amount, price, total_fee, pay_fee, " +
            "product_type, goods_type, num, cabinet_code, exchange_flag, exchange_amount, gift_flag, " +
            "outbound_flag, erp_store_code, tenant_id, etl_time, " +
            "creator, create_time, updater, update_time, deleted) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.subOrderId}, #{item.orderId}, #{item.skuCode}, #{item.subSkuCode}, #{item.productSkuId}, " +
            "#{item.skuName}, #{item.barcode}, #{item.specification}, #{item.weight}, #{item.totalWeight}, " +
            "#{item.buyAmount}, #{item.price}, #{item.totalFee}, #{item.payFee}, " +
            "#{item.productType}, #{item.goodsType}, #{item.num}, #{item.cabinetCode}, " +
            "#{item.exchangeFlag}, #{item.exchangeAmount}, #{item.giftFlag}, " +
            "#{item.outboundFlag}, #{item.erpStoreCode}, #{item.tenantId}, #{item.etlTime}, " +
            "#{item.creator}, #{item.createTime}, #{item.updater}, #{item.updateTime}, #{item.deleted})" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "sku_code = VALUES(sku_code), sub_sku_code = VALUES(sub_sku_code), " +
            "product_sku_id = VALUES(product_sku_id), sku_name = VALUES(sku_name), " +
            "barcode = VALUES(barcode), specification = VALUES(specification), " +
            "weight = VALUES(weight), total_weight = VALUES(total_weight), " +
            "buy_amount = VALUES(buy_amount), price = VALUES(price), " +
            "total_fee = VALUES(total_fee), pay_fee = VALUES(pay_fee), " +
            "product_type = VALUES(product_type), goods_type = VALUES(goods_type), " +
            "num = VALUES(num), cabinet_code = VALUES(cabinet_code), " +
            "exchange_flag = VALUES(exchange_flag), exchange_amount = VALUES(exchange_amount), " +
            "gift_flag = VALUES(gift_flag), outbound_flag = VALUES(outbound_flag), " +
            "erp_store_code = VALUES(erp_store_code), tenant_id = VALUES(tenant_id), " +
            "etl_time = VALUES(etl_time), " +
            "updater = VALUES(updater), update_time = VALUES(update_time), " +
            "deleted = VALUES(deleted)" +
            "</script>")
    int upsertBatch(@Param("items") List<OrderItemDO> items);
}
