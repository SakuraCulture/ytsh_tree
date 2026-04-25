package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;

/**
 * 饿了么 API 限流器接口
 * 用于控制调用翱象接口的频率，避免超出平台限流阈值
 */
public interface EleApiRateLimiter {

    /**
     * 调用订单列表接口（限流 400 QPS）
     *
     * @param req 请求参数
     * @return 订单列表响应
     */
    OrderListRespDTO callOrderList(OrderListReqDTO req);

    /**
     * 调用订单详情接口（限流 200 QPS）
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderListRespDTO.OrderDetail callOrderDetail(String orderId);

}