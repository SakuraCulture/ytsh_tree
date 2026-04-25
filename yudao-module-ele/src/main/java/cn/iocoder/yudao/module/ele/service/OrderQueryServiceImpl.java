package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.order.vo.OrderListReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.*;
import cn.iocoder.yudao.module.ele.dal.mysql.*;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderPlatformMapper orderPlatformMapper;

    @Resource
    private OrderDiscountMapper orderDiscountMapper;

    @Override
    public OrderListRespDTO getOrderList(OrderListReqVO reqVO) {
        String storeCode = reqVO.getErpStoreCode();
        if (storeCode == null || storeCode.isEmpty()) {
            storeCode = reqVO.getPlatformStoreId();
        }

        Long startTime = reqVO.getStartTime();
        Long endTime = reqVO.getEndTime();

        List<OrderDO> orderList = orderMapper.selectListByStoreCodeAndTime(
                storeCode, reqVO.getStatus(), startTime, endTime);

        Long total = orderMapper.countByStoreCodeAndTime(
                storeCode, reqVO.getStatus(), startTime, endTime);

        List<OrderListRespDTO.OrderDetail> voList = assembleOrderList(orderList);

        OrderListRespDTO result = new OrderListRespDTO();
        result.setTotal(total);
        result.setScrollId(null);
        result.setOrderList(voList);

        return result;
    }

    private List<OrderListRespDTO.OrderDetail> assembleOrderList(List<OrderDO> orderList) {
        if (orderList == null || orderList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderIds = orderList.stream()
                .map(OrderDO::getOrderId)
                .collect(Collectors.toList());

        Map<String, List<OrderItemDO>> itemMap = orderItemMapper.selectByOrderIds(orderIds)
                .stream()
                .collect(Collectors.groupingBy(OrderItemDO::getOrderId));

        Map<String, OrderPlatformDO> platformMap = orderPlatformMapper.selectByOrderIds(orderIds)
                .stream()
                .collect(Collectors.toMap(OrderPlatformDO::getOrderId, Function.identity(), (a, b) -> a));

        Map<String, List<OrderDiscountDO>> discountMap = orderDiscountMapper.selectByOrderIds(orderIds)
                .stream()
                .collect(Collectors.groupingBy(OrderDiscountDO::getOrderId));

        return orderList.stream().map(order -> {
            OrderListRespDTO.OrderDetail vo = convertToVO(order);

            vo.setSubOrders(convertItems(itemMap.get(order.getOrderId())));
            vo.setDiscounts(convertDiscounts(discountMap.get(order.getOrderId())));

            OrderPlatformDO platform = platformMap.get(order.getOrderId());
            if (platform != null) {
                vo.setDeliveryPlatform(platform.getDeliveryPlatform() != null ? String.valueOf(platform.getDeliveryPlatform()) : null);
                vo.setDeliveryType(platform.getDeliveryType());
                vo.setPlatformCommissionFee(yuanToFen(platform.getPlatformCommissionFee()));
            }

            return vo;
        }).collect(Collectors.toList());
    }

    private OrderListRespDTO.OrderDetail convertToVO(OrderDO order) {
        OrderListRespDTO.OrderDetail vo = new OrderListRespDTO.OrderDetail();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setChannelSourceName(order.getChannelSourceName());
        vo.setBuyerName(order.getBuyerName());
        vo.setBuyerPhone(order.getBuyerPhone());
        vo.setBuyerAddress(order.getBuyerAddress());
        vo.setDeliveryName(order.getDeliveryName());
        vo.setDeliveryPhone(order.getDeliveryPhone());
        vo.setDeliveryStatus(order.getDeliveryStatus());
        vo.setTotalFee(yuanToFen(order.getTotalFee()));
        vo.setPayFee(yuanToFen(order.getPayFee()));
        vo.setDiscountFee(yuanToFen(order.getDiscountFee()));
        vo.setDeliveryFee(yuanToFen(order.getDeliveryFee()));
        vo.setPostFee(yuanToFen(order.getPostFee()));
        vo.setPackageFee(yuanToFen(order.getPackageFee()));
        vo.setRemark(order.getRemark());
        vo.setChannelSourceId(order.getChannelSourceId());
        vo.setChannelOrderId(order.getChannelOrderId());
        vo.setStoreCode(order.getStoreCode());
        vo.setErpStoreCode(order.getStoreCode());
        vo.setLongitude(order.getLongitude());
        vo.setLatitude(order.getLatitude());
        return vo;
    }

    private List<OrderDetailRespDTO.SubOrder> convertItems(List<OrderItemDO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(item -> {
            OrderDetailRespDTO.SubOrder vo = new OrderDetailRespDTO.SubOrder();
            vo.setSubOrderId(item.getSubOrderId());
            vo.setSkuCode(item.getSkuCode());
            vo.setSkuName(item.getSkuName());
            vo.setBarcode(item.getBarcode());
            vo.setSpecification(item.getSpecification());
            vo.setWeight(item.getWeight() != null ? item.getWeight().intValue() : null);
            vo.setBuyAmount(item.getBuyAmount());
            vo.setNum(item.getNum());
            vo.setPrice(yuanToFen(item.getPrice()));
            vo.setTotalFee(yuanToFen(item.getTotalFee()));
            vo.setPayFee(yuanToFen(item.getPayFee()));
            vo.setGoodsType(convertProductType(item.getProductType()));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<OrderDetailRespDTO.Discount> convertDiscounts(List<OrderDiscountDO> discounts) {
        if (discounts == null || discounts.isEmpty()) {
            return Collections.emptyList();
        }
        return discounts.stream().map(discount -> {
            OrderDetailRespDTO.Discount vo = new OrderDetailRespDTO.Discount();
            vo.setActivityName(discount.getActivityName());
            vo.setActivityId(discount.getActivityId());
            vo.setType(discount.getDiscountType());
            vo.setDiscountFee(yuanToFen(discount.getDiscountFee()));
            vo.setMerchantFee(yuanToFen(discount.getMerchantFee()));
            vo.setPlatformFee(yuanToFen(discount.getPlatformFee()));
            return vo;
        }).collect(Collectors.toList());
    }

    private Integer yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(new BigDecimal("100")).intValue();
    }

    private String convertProductType(Integer productType) {
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