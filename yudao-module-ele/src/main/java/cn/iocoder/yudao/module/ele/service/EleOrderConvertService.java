package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderDiscountDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderItemDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.OrderPlatformDO;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderDiscountMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderItemMapper;
import cn.iocoder.yudao.module.ele.dal.mysql.OrderPlatformMapper;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO.SubOrder;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO.Discount;
import lib.ele.retail.param.SaasOrderGetResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EleOrderConvertService {

    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private OrderPlatformMapper orderPlatformMapper;
    @Resource
    private OrderDiscountMapper orderDiscountMapper;

    public List<OrderListRespDTO.OrderDetail> assembleOrderList(List<OrderDO> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> orderIds = orders.stream().map(OrderDO::getOrderId).collect(Collectors.toList());
        Map<String, List<OrderItemDO>> itemMap = orderItemMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemDO::getOrderId));
        Map<String, OrderPlatformDO> platformMap = orderPlatformMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(OrderPlatformDO::getOrderId, v -> v, (a, b) -> a));
        Map<String, List<OrderDiscountDO>> discountMap = orderDiscountMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderDiscountDO::getOrderId));

        return orders.stream().map(order -> {
            OrderListRespDTO.OrderDetail detail = convertToOrderDetail(order);
            detail.setSubOrders(convertItems(itemMap.get(order.getOrderId())));
            detail.setDiscounts(convertDiscounts(discountMap.get(order.getOrderId())));
            OrderPlatformDO platform = platformMap.get(order.getOrderId());
            if (platform != null) {
                detail.setDeliveryPlatform(
                        platform.getDeliveryPlatform() == null ? null : String.valueOf(platform.getDeliveryPlatform()));
                detail.setDeliveryType(platform.getDeliveryType());
                detail.setPlatformCommissionFee(yuanToFen(platform.getPlatformCommissionFee()));
                detail.setChannelType(platform.getPlatformType());
            }
            return detail;
        }).collect(Collectors.toList());
    }

    public OrderListRespDTO.OrderDetail convertToOrderDetail(OrderDO order) {
        OrderListRespDTO.OrderDetail detail = new OrderListRespDTO.OrderDetail();
        detail.setOrderId(order.getOrderId());
        detail.setStatus(order.getOrderStatus());
        detail.setCreateTime(order.getCreateTime());
        detail.setPayTime(order.getPayTime());
        detail.setChannelSourceName(order.getChannelSourceName());
        detail.setBuyerName(order.getBuyerName());
        detail.setBuyerPhone(order.getBuyerPhone());
        detail.setBuyerAddress(order.getBuyerAddress());
        detail.setDeliveryName(order.getDeliveryName());
        detail.setDeliveryPhone(order.getDeliveryPhone());
        detail.setDeliveryStatus(order.getDeliveryStatus());
        detail.setTotalFee(yuanToFen(order.getTotalFee()));
        detail.setPayFee(yuanToFen(order.getPayFee()));
        detail.setDiscountFee(yuanToFen(order.getDiscountFee()));
        detail.setDeliveryFee(yuanToFen(order.getDeliveryFee()));
        detail.setPostFee(yuanToFen(order.getPostFee()));
        detail.setPackageFee(yuanToFen(order.getPackageFee()));
        detail.setRemark(order.getRemark());
        detail.setChannelSourceId(order.getChannelSourceId());
        detail.setChannelOrderId(order.getChannelOrderId());
        detail.setStoreCode(order.getStoreCode());
        detail.setErpStoreCode(order.getStoreCode());
        detail.setLongitude(order.getLongitude());
        detail.setLatitude(order.getLatitude());
        detail.setUserId(order.getUserId());
        detail.setArriveType(order.getArriveType());
        return detail;
    }

    public OrderDetailRespDTO assembleOrderDetail(OrderDO order) {
        OrderDetailRespDTO dto = new OrderDetailRespDTO();
        dto.setOrderId(order.getOrderId());
        dto.setStatus(order.getOrderStatus());
        dto.setCreateTime(order.getCreateTime());
        dto.setPayTime(order.getPayTime());
        dto.setChannelSourceName(order.getChannelSourceName());
        dto.setBuyerName(order.getBuyerName());
        dto.setBuyerPhone(order.getBuyerPhone());
        dto.setBuyerAddress(order.getBuyerAddress());
        dto.setDeliveryName(order.getDeliveryName());
        dto.setDeliveryPhone(order.getDeliveryPhone());
        dto.setDeliveryStatus(order.getDeliveryStatus());
        dto.setTotalFee(yuanToFen(order.getTotalFee()));
        dto.setPayFee(yuanToFen(order.getPayFee()));
        dto.setDiscountFee(yuanToFen(order.getDiscountFee()));
        dto.setDeliveryFee(yuanToFen(order.getDeliveryFee()));
        dto.setPostFee(yuanToFen(order.getPostFee()));
        dto.setPackageFee(yuanToFen(order.getPackageFee()));
        dto.setRemark(order.getRemark());
        dto.setChannelSourceId(order.getChannelSourceId());
        dto.setChannelOrderId(order.getChannelOrderId());
        dto.setStoreCode(order.getStoreCode());
        dto.setLongitude(order.getLongitude());
        dto.setLatitude(order.getLatitude());
        dto.setUserId(order.getUserId());
        dto.setArriveType(order.getArriveType());

        List<OrderPlatformDO> platformList = orderPlatformMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<OrderPlatformDO>()
                        .eq(OrderPlatformDO::getOrderId, order.getOrderId())
                        .eq(OrderPlatformDO::getDeleted, false)
                        .last("LIMIT 1"));
        OrderPlatformDO platform = platformList != null && !platformList.isEmpty() ? platformList.get(0) : null;
        if (platform != null) {
            dto.setDeliveryPlatform(
                    platform.getDeliveryPlatform() == null ? null : String.valueOf(platform.getDeliveryPlatform()));
            dto.setDeliveryType(platform.getDeliveryType());
            dto.setPlatformCommissionFee(yuanToFen(platform.getPlatformCommissionFee()));
            dto.setChannelType(platform.getPlatformType());
        }

        dto.setSubOrders(convertItems(orderItemMapper.selectByOrderIds(List.of(order.getOrderId()))));
        dto.setDiscounts(convertDiscounts(orderDiscountMapper.selectByOrderIds(List.of(order.getOrderId()))));
        return dto;
    }

    public List<OrderDetailRespDTO.SubOrder> convertItems(List<OrderItemDO> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        return items.stream().map(item -> {
            OrderDetailRespDTO.SubOrder subOrder = new OrderDetailRespDTO.SubOrder();
            subOrder.setSubOrderId(item.getSubOrderId());
            subOrder.setSkuCode(item.getSkuCode());
            subOrder.setSkuName(item.getSkuName());
            subOrder.setBarcode(item.getBarcode());
            subOrder.setSpecification(item.getSpecification());
            subOrder.setWeight(item.getWeight() == null ? null : item.getWeight().intValue());
            subOrder.setBuyAmount(item.getBuyAmount());
            subOrder.setNum(item.getNum());
            subOrder.setPrice(yuanToFen(item.getPrice()));
            subOrder.setTotalFee(yuanToFen(item.getTotalFee()));
            subOrder.setPayFee(yuanToFen(item.getPayFee()));
            subOrder.setGoodsType(convertProductType(item.getProductType()));
            return subOrder;
        }).collect(Collectors.toList());
    }

    public List<OrderDetailRespDTO.Discount> convertDiscounts(List<OrderDiscountDO> discounts) {
        if (discounts == null || discounts.isEmpty()) {
            return new ArrayList<>();
        }
        return discounts.stream().map(discount -> {
            OrderDetailRespDTO.Discount dto = new OrderDetailRespDTO.Discount();
            dto.setActivityName(discount.getActivityName());
            dto.setActivityId(discount.getActivityId());
            dto.setType(discount.getDiscountType());
            dto.setDiscountFee(yuanToFen(discount.getDiscountFee()));
            dto.setMerchantFee(yuanToFen(discount.getMerchantFee()));
            dto.setPlatformFee(yuanToFen(discount.getPlatformFee()));
            return dto;
        }).collect(Collectors.toList());
    }

    public OrderListRespDTO.OrderDetail convertToOrderListDetail(OrderDetailRespDTO detailRespDTO) {
        OrderListRespDTO.OrderDetail detail = new OrderListRespDTO.OrderDetail();
        detail.setOrderId(detailRespDTO.getOrderId());
        detail.setStatus(detailRespDTO.getStatus());
        detail.setCreateTime(detailRespDTO.getCreateTime());
        detail.setPayTime(detailRespDTO.getPayTime());
        detail.setChannelSourceName(detailRespDTO.getChannelSourceName());
        detail.setBuyerName(detailRespDTO.getBuyerName());
        detail.setBuyerPhone(detailRespDTO.getBuyerPhone());
        detail.setBuyerAddress(detailRespDTO.getBuyerAddress());
        detail.setDeliveryName(detailRespDTO.getDeliveryName());
        detail.setDeliveryPhone(detailRespDTO.getDeliveryPhone());
        detail.setDeliveryPlatform(detailRespDTO.getDeliveryPlatform());
        detail.setDeliveryType(detailRespDTO.getDeliveryType());
        detail.setDeliveryStatus(detailRespDTO.getDeliveryStatus());
        detail.setTotalFee(detailRespDTO.getTotalFee());
        detail.setPayFee(detailRespDTO.getPayFee());
        detail.setDiscountFee(detailRespDTO.getDiscountFee());
        detail.setDeliveryFee(detailRespDTO.getDeliveryFee());
        detail.setPostFee(detailRespDTO.getPostFee());
        detail.setPackageFee(detailRespDTO.getPackageFee());
        detail.setPlatformCommissionFee(detailRespDTO.getPlatformCommissionFee());
        detail.setRemark(detailRespDTO.getRemark());
        detail.setChannelSourceId(detailRespDTO.getChannelSourceId());
        detail.setChannelOrderId(detailRespDTO.getChannelOrderId());
        detail.setChannelType(detailRespDTO.getChannelType());
        detail.setStoreCode(detailRespDTO.getStoreCode());
        detail.setErpStoreCode(detailRespDTO.getErpStoreCode());
        detail.setLongitude(detailRespDTO.getLongitude());
        detail.setLatitude(detailRespDTO.getLatitude());
        detail.setUserId(detailRespDTO.getUserId());
        detail.setArriveType(detailRespDTO.getArriveType());
        detail.setSubOrders(detailRespDTO.getSubOrders());
        detail.setDiscounts(detailRespDTO.getDiscounts());
        return detail;
    }

    public void fillOrderDetail(OrderDetailRespDTO dto, SaasOrderGetResult.SaasOrderGetData data) {
        dto.setOrderId(data.getOrder_id());
        dto.setCreateTime(data.getCreate_time());
        dto.setPayTime(data.getPay_time());
        dto.setChannelSourceName(data.getChannel_source_name());
        dto.setBuyerName(data.getBuyer_name());
        dto.setBuyerPhone(data.getBuyer_phone());
        dto.setBuyerAddress(data.getBuyer_address());
        dto.setDeliveryName(data.getDelivery_name());
        dto.setDeliveryPhone(data.getDelivery_phone());
        dto.setDeliveryPlatform(data.getDelivery_platform());
        dto.setDeliveryType(data.getDelivery_type());
        dto.setDeliveryStatus(data.getDelivery_status());
        dto.setStatus(data.getStatus());
        dto.setTotalFee(data.getTotal_fee());
        dto.setPayFee(data.getPay_fee());
        dto.setDiscountFee(data.getDiscount_fee());
        dto.setDeliveryFee(data.getDelivery_fee());
        dto.setPostFee(data.getPost_fee());
        dto.setPackageFee(data.getPackage_fee());
        dto.setPlatformCommissionFee(data.getPlatform_commission_fee());
        dto.setRemark(data.getRemark());
        dto.setChannelSourceId(data.getChannel_source_id());
        dto.setChannelOrderId(data.getChannel_order_id());
        dto.setChannelType(data.getChannel_type());
        dto.setStoreCode(data.getStore_code());
        dto.setErpStoreCode(data.getErp_store_code());
        dto.setLongitude(data.getLongitude());
        dto.setLatitude(data.getLatitude());
        dto.setUserId(data.getUser_id());
        dto.setArriveType(data.getArrive_type());

        if (data.getSub_orders() != null) {
            for (SaasOrderGetResult.SubOrder subOrder : data.getSub_orders()) {
                OrderDetailRespDTO.SubOrder subOrderDTO = new OrderDetailRespDTO.SubOrder();
                subOrderDTO.setSubOrderId(subOrder.getSub_order_id());
                subOrderDTO.setSkuCode(subOrder.getSku_code());
                subOrderDTO.setSkuName(subOrder.getSku_name());
                subOrderDTO.setBarcode(subOrder.getBarcode());
                subOrderDTO.setSpecification(subOrder.getSpecification());
                subOrderDTO.setPrice(subOrder.getPrice());
                subOrderDTO.setTotalFee(subOrder.getTotal_fee());
                subOrderDTO.setPayFee(subOrder.getPay_fee());
                subOrderDTO.setBuyAmount(subOrder.getBuy_amount());
                subOrderDTO.setGoodsType(subOrder.getGoods_type());
                subOrderDTO.setCabinetCode(subOrder.getCabinet_code());
                subOrderDTO.setWeight(subOrder.getWeight());
                dto.getSubOrders().add(subOrderDTO);
            }
        }

        if (data.getDiscounts() != null) {
            for (SaasOrderGetResult.Discount discount : data.getDiscounts()) {
                OrderDetailRespDTO.Discount discountDTO = new OrderDetailRespDTO.Discount();
                discountDTO.setActivityName(discount.getActivity_name());
                discountDTO.setActivityId(discount.getActivity_id());
                discountDTO.setType(discount.getType());
                discountDTO.setDiscountFee(discount.getDiscount_fee());
                discountDTO.setMerchantFee(discount.getMerchant_fee());
                discountDTO.setPlatformFee(discount.getPlatform_fee());
                dto.getDiscounts().add(discountDTO);
            }
        }
    }

    public OrderDetailRespDTO convertDetailResult(SaasOrderGetResult result) {
        SaasOrderGetResult.SaasOrderGetData data = result.getData();
        if (data == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        OrderDetailRespDTO dto = new OrderDetailRespDTO();
        fillOrderDetail(dto, data);

        return dto;
    }

    public Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Integer.valueOf(value);
    }

    public String resolveStoreCode(OrderListRespDTO.OrderDetail detail, String erpStoreCode) {
        if (erpStoreCode != null && !erpStoreCode.isEmpty()) {
            return erpStoreCode;
        }
        if (detail.getErpStoreCode() != null && !detail.getErpStoreCode().isEmpty()) {
            return detail.getErpStoreCode();
        }
        return detail.getStoreCode();
    }

    public BigDecimal fenToYuan(Integer fen) {
        if (fen == null) {
            return null;
        }
        return new BigDecimal(fen).divide(new BigDecimal("100"));
    }

    public Integer yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(new BigDecimal("100")).intValue();
    }

    public String convertProductType(Integer productType) {
        if (productType == null) {
            return null;
        }
        return switch (productType) {
            case 0 -> "0";
            case 1 -> "3";
            default -> null;
        };
    }
}