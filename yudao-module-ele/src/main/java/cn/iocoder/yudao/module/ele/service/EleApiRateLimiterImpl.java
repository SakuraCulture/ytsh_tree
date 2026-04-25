package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class EleApiRateLimiterImpl implements EleApiRateLimiter {

    private static final String ORDER_LIST_RATE_LIMITER_KEY = "rate_limiter:ele:order_list";
    private static final String ORDER_GET_RATE_LIMITER_KEY = "rate_limiter:ele:order_detail";

    private static final long ORDER_LIST_RATE = 400;
    private static final long ORDER_GET_RATE = 200;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public OrderListRespDTO callOrderList(OrderListReqDTO req) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(ORDER_LIST_RATE_LIMITER_KEY);
        rateLimiter.trySetRate(RateType.OVERALL, ORDER_LIST_RATE, Duration.ofSeconds(1));

        if (!rateLimiter.tryAcquire()) {
            log.warn("【限流】订单列表接口被限流，拒绝请求");
            throw new RuntimeException("订单列表接口调用频率超限，请稍后重试");
        }

        log.debug("【限流】订单列表接口获取令牌成功");
        return null;
    }

    @Override
    public OrderListRespDTO.OrderDetail callOrderDetail(String orderId) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(ORDER_GET_RATE_LIMITER_KEY);
        rateLimiter.trySetRate(RateType.OVERALL, ORDER_GET_RATE, Duration.ofSeconds(1));

        if (!rateLimiter.tryAcquire()) {
            log.warn("【限流】订单详情接口被限流，拒绝请求");
            throw new RuntimeException("订单详情接口调用频率超限，请稍后重试");
        }

        log.debug("【限流】订单详情接口获取令牌成功");
        return null;
    }
}