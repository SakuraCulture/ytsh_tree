package cn.iocoder.yudao.module.ele.mq;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import cn.iocoder.yudao.module.ele.service.dto.OrderStatusPushMessage;
import cn.iocoder.yudao.module.ele.service.push.OrderStatusPushService;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import lib.ele.retail.param.SaasOrderGetParam;
import lib.ele.retail.param.SaasOrderGetResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@Lazy(false)
public class SaasOrderStatusPushConsumer {

    @PostConstruct
    public void init() {
        log.info("【SaaS推送消费】==== 消费者Bean初始化完成，pushEnabled={} ====", pushEnabled);
        log.info("【SaaS推送消费】消费者配置：topic={}, groupId={}, idempotentKeyPrefix={}, expireSeconds={}",
                "order-status-change", "order-status-consumer", idempotentKeyPrefix, expireSeconds);
        log.info("【SaaS推送消费】如果pushEnabled=false，消费者将跳过所有消息处理");
    }

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private EleOpenApiClient eleOpenApiClient;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private OrderStatusPushService orderStatusPushService;

    @Resource
    private EleApiConfigMapper eleApiConfigMapper;

    @Value("${ele.saas.push.redis.idempotent-key-prefix:order:push:consume:}")
    private String idempotentKeyPrefix;

    @Value("${ele.saas.push.redis.expire-seconds:3600}")
    private int expireSeconds;

    @Value("${ele.saas.push.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${ele.saas.push.retry.retry-interval-ms:2000}")
    private long retryIntervalMs;

    @Value("${ele.saas.push.enabled}")
    private boolean pushEnabled;

    @KafkaListener(topics = "${ele.saas.push.kafka.topic:order-status-change}", groupId = "${ele.saas.push.kafka.consumer-group-id:order-status-consumer}", containerFactory = "saasPushKafkaListenerContainerFactory")
    public void consumePushMessage(
            OrderStatusPushMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        if (!pushEnabled) {
            log.debug("【SaaS推送消费】订单推送功能已关闭，跳过消息处理，orderId={}", message.getOrderId());
            acknowledgment.acknowledge();
            return;
        }

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        String cmd = message.getCmd();

        log.info("【SaaS推送消费】==== Kafka消费 START，orderId={}, cmd={}, ticket={}, partition={}, offset={} ====",
                orderId, cmd, ticket, partition, offset);

        if ("saas.order.create".equals(cmd)) {
            consumeOrderCreateMessage(message, partition, offset, acknowledgment);
        } else if ("saas.order.status.push".equals(cmd)) {
            consumeOrderStatusChangeMessage(message, partition, offset, acknowledgment);
        } else if ("saas.order.remind.push".equals(cmd)) {
            consumeOrderRemindMessage(message, partition, offset, acknowledgment);
        } else if ("saas.order.deliveryStatus.push".equals(cmd)) {
            consumeOrderDeliveryStatusChangeMessage(message, partition, offset, acknowledgment);
        } else if ("saas.supply.chain.execution.order.msg".equals(cmd)) {
            consumeSupplyChainMessage(message, partition, offset, acknowledgment);
        } else if ("saas.reverse.order.status.push".equals(cmd)) {
            consumeReverseOrderStatusChangeMessage(message, partition, offset, acknowledgment);
        } else {
            log.warn("【SaaS推送消费】未知的cmd类型，cmd={}, orderId={}", cmd, orderId);
            acknowledgment.acknowledge();
        }
    }

    private void consumeOrderCreateMessage(
            OrderStatusPushMessage message,
            int partition,
            long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        log.info("【订单创建消费】==== 开始处理订单创建 ====");
        log.info("【订单创建消费】orderId={}, platformStoreId={}, ticket={}, partition={}, offset={}",
                orderId, message.getPlatformStoreId(), ticket, partition, offset);

        log.info("【订单创建消费】步骤1: 幂等检查，ticket={}", ticket);
        if (!checkIdempotent(ticket)) {
            log.info("【订单创建消费】步骤1: 幂等检查命中，跳过处理，orderId={}, ticket={}", orderId, ticket);
            acknowledgment.acknowledge();
            return;
        }
        log.info("【订单创建消费】步骤1: 幂等检查通过 ✓");

        try {
            log.info("【订单创建消费】步骤2: 补充merchantCode/erpStoreCode...");
            if (StrUtil.isBlank(message.getMerchantCode())) {
                EleApiConfig config = eleApiConfigMapper.selectActive();
                if (config != null) {
                    message.setMerchantCode(config.getMerchantCode());
                    log.info("【订单创建消费】步骤2: 从ele_api_config表补充merchantCode={}", config.getMerchantCode());
                } else {
                    log.warn("【订单创建消费】步骤2: ele_api_config表无启用记录，merchantCode仍为空");
                }
            }
            if (StrUtil.isBlank(message.getErpStoreCode()) && message.getPlatformStoreId() != null) {
                message.setErpStoreCode(message.getPlatformStoreId());
                log.info("【订单创建消费】步骤2: 补充erpStoreCode=platformStoreId={}", message.getPlatformStoreId());
            }
            log.info("【订单创建消费】步骤2: 补充完成，merchantCode={}, erpStoreCode={}",
                    message.getMerchantCode(), message.getErpStoreCode());

            log.info("【订单创建消费】步骤3: 调用翱象API拉取订单详情，orderId={}...", orderId);
            OrderDetailRespDTO orderDetail = fetchOrderDetailWithRetry(message);
            if (orderDetail == null) {
                log.error("【订单创建消费】步骤3: 翱象API返回订单详情为空，orderId={}", orderId);
                throw new RuntimeException("订单详情不存在，orderId=" + orderId);
            }
            log.info("【订单创建消费】步骤3: 翱象API返回成功 ✓，orderId={}, status={}, buyerName={}, totalFee={}",
                    orderDetail.getOrderId(), orderDetail.getStatus(), orderDetail.getBuyerName(),
                    orderDetail.getTotalFee());

            log.info("【订单创建消费】步骤4: 入库保存订单，orderId={}...", orderId);
            OrderMessage orderMessage = convertToOrderMessage(orderDetail, message);
            orderMessage.setCreateTime(orderDetail.getCreateTime());
            eleOrderService.consumeOrderMessage(orderMessage, true);
            log.info("【订单创建消费】步骤4: 订单入库成功 ✓，orderId={}", orderId);

            log.info("【订单创建消费】步骤5: 推送WebSocket通知，orderId={}, newStatus={}...", orderId, orderDetail.getStatus());
            orderStatusPushService.pushOrderStatusChange(
                    orderId,
                    null,
                    orderDetail.getStatus(),
                    message.getPlatformStoreId(),
                    orderDetail.getBuyerName());
            log.info("【订单创建消费】步骤5: WebSocket推送完成 ✓");

            acknowledgment.acknowledge();
            log.info("【订单创建消费】==== 订单创建处理完成 END，orderId={} ====", orderId);

        } catch (Exception e) {
            log.error("【SaaS订单创建消费】消费失败，orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private void consumeOrderStatusChangeMessage(
            OrderStatusPushMessage message,
            int partition,
            long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        log.info("【状态变更消费】==== 开始处理订单状态变更 ====");
        log.info("【状态变更消费】orderId={}, status={}, ticket={}, partition={}, offset={}",
                orderId, message.getStatus(), ticket, partition, offset);

        log.info("【状态变更消费】步骤1: 幂等检查，ticket={}", ticket);
        if (!checkIdempotent(ticket)) {
            log.info("【状态变更消费】步骤1: 幂等检查命中，跳过处理，orderId={}, ticket={}", orderId, ticket);
            acknowledgment.acknowledge();
            return;
        }
        log.info("【状态变更消费】步骤1: 幂等检查通过 ✓");

        try {
            log.info("【状态变更消费】步骤1.5: 补充merchantCode/erpStoreCode...");
            if (StrUtil.isBlank(message.getMerchantCode())) {
                EleApiConfig config = eleApiConfigMapper.selectActive();
                if (config != null) {
                    message.setMerchantCode(config.getMerchantCode());
                    log.info("【状态变更消费】步骤1.5: 从ele_api_config表补充merchantCode={}", config.getMerchantCode());
                } else {
                    log.warn("【状态变更消费】步骤1.5: ele_api_config表无启用记录，merchantCode仍为空");
                }
            }
            if (StrUtil.isBlank(message.getErpStoreCode()) && message.getPlatformStoreId() != null) {
                message.setErpStoreCode(message.getPlatformStoreId());
                log.info("【状态变更消费】步骤1.5: 补充erpStoreCode=platformStoreId={}", message.getPlatformStoreId());
            }
            log.info("【状态变更消费】步骤1.5: 补充完成，merchantCode={}, erpStoreCode={}",
                    message.getMerchantCode(), message.getErpStoreCode());

            log.info("【状态变更消费】步骤2: 查询本地数据库获取旧状态，orderId={}...", orderId);
            OrderDetailRespDTO existingOrder = eleOrderService.getOrderDetail(
                    null, message.getMerchantCode(), message.getErpStoreCode(), orderId);
            Integer oldStatus = existingOrder != null ? existingOrder.getStatus() : null;
            log.info("【状态变更消费】步骤2: 本地旧状态={}，orderId={}", oldStatus, orderId);

            log.info("【状态变更消费】步骤3: 调用翱象API拉取最新订单详情，orderId={}...", orderId);
            OrderDetailRespDTO orderDetail = fetchOrderDetailWithRetry(message);
            if (orderDetail == null) {
                log.error("【状态变更消费】步骤3: 翱象API返回订单详情为空，orderId={}", orderId);
                throw new RuntimeException("订单详情不存在，orderId=" + orderId);
            }
            log.info("【状态变更消费】步骤3: 翱象API返回成功 ✓，orderId={}, status={}, buyerName={}",
                    orderDetail.getOrderId(), orderDetail.getStatus(), orderDetail.getBuyerName());

            log.info("【状态变更消费】步骤4: 更新订单入库，orderId={}, oldStatus={} -> newStatus={}...",
                    orderId, oldStatus, orderDetail.getStatus());
            OrderMessage orderMessage = convertToOrderMessage(orderDetail, message);
            eleOrderService.consumeOrderMessage(orderMessage, true);
            log.info("【状态变更消费】步骤4: 订单更新入库成功 ✓，orderId={}", orderId);

            Integer finalOldStatus = oldStatus != null ? oldStatus : message.getStatus();
            log.info("【状态变更消费】步骤5: 推送WebSocket通知，orderId={}, oldStatus={}, newStatus={}...",
                    orderId, finalOldStatus, orderDetail.getStatus());
            orderStatusPushService.pushOrderStatusChange(
                    orderId,
                    finalOldStatus,
                    orderDetail.getStatus(),
                    message.getErpStoreCode(),
                    orderDetail.getBuyerName());
            log.info("【状态变更消费】步骤5: WebSocket推送完成 ✓");

            acknowledgment.acknowledge();
            log.info("【状态变更消费】==== 订单状态变更处理完成 END，orderId={}, oldStatus={} -> newStatus={} ====",
                    orderId, finalOldStatus, orderDetail.getStatus());

        } catch (Exception e) {
            log.error("【SaaS推送消费】消费失败，orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private void consumeOrderRemindMessage(
            OrderStatusPushMessage message,
            int partition,
            long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        log.info("【催单消费】==== 开始处理催单推送 ====");
        log.info("【催单消费】orderId={}, ticket={}, partition={}, offset={}",
                orderId, ticket, partition, offset);

        log.info("【催单消费】步骤1: 幂等检查，ticket={}", ticket);
        if (!checkIdempotent(ticket)) {
            log.info("【催单消费】步骤1: 幂等检查命中，跳过处理，orderId={}, ticket={}", orderId, ticket);
            acknowledgment.acknowledge();
            return;
        }
        log.info("【催单消费】步骤1: 幂等检查通过 ✓");

        try {
            log.info("【催单消费】步骤2: 查询本地订单信息，orderId={}...", orderId);
            OrderDetailRespDTO existingOrder = eleOrderService.getOrderDetail(
                    null, message.getMerchantCode(), message.getErpStoreCode(), orderId);

            String buyerName = existingOrder != null ? existingOrder.getBuyerName() : null;
            String storeName = existingOrder != null ? existingOrder.getChannelSourceName() : null;
            String channelOrderId = message.getChannelOrderId();
            if (channelOrderId == null && existingOrder != null) {
                channelOrderId = existingOrder.getChannelOrderId();
            }
            log.info("【催单消费】步骤2: 本地订单信息 ✓，orderId={}, buyerName={}, storeName={}",
                    orderId, buyerName, storeName);

            log.info("【催单消费】步骤3: 推送催单WebSocket通知，orderId={}...", orderId);
            orderStatusPushService.pushOrderRemind(
                    orderId,
                    channelOrderId,
                    storeName,
                    buyerName,
                    1);
            log.info("【催单消费】步骤3: WebSocket推送完成 ✓");

            acknowledgment.acknowledge();
            log.info("【催单消费】==== 催单推送处理完成 END，orderId={} ====", orderId);

        } catch (Exception e) {
            log.error("【催单消费】消费失败，orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private void consumeOrderDeliveryStatusChangeMessage(
            OrderStatusPushMessage message,
            int partition,
            long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        log.info("【配送状态消费】==== 开始处理配送状态变更 ====");
        log.info("【配送状态消费】orderId={}, status={}, ticket={}, partition={}, offset={}",
                orderId, message.getStatus(), ticket, partition, offset);

        log.info("【配送状态消费】步骤1: 幂等检查，ticket={}", ticket);
        if (!checkIdempotent(ticket)) {
            log.info("【配送状态消费】步骤1: 幂等检查命中，跳过处理，orderId={}, ticket={}", orderId, ticket);
            acknowledgment.acknowledge();
            return;
        }
        log.info("【配送状态消费】步骤1: 幂等检查通过 ✓");

        try {
            log.info("【配送状态消费】步骤1.5: 补充merchantCode/erpStoreCode...");
            if (StrUtil.isBlank(message.getMerchantCode())) {
                EleApiConfig config = eleApiConfigMapper.selectActive();
                if (config != null) {
                    message.setMerchantCode(config.getMerchantCode());
                    log.info("【配送状态消费】步骤1.5: 从ele_api_config表补充merchantCode={}", config.getMerchantCode());
                } else {
                    log.warn("【配送状态消费】步骤1.5: ele_api_config表无启用记录，merchantCode仍为空");
                }
            }
            if (StrUtil.isBlank(message.getErpStoreCode()) && message.getPlatformStoreId() != null) {
                message.setErpStoreCode(message.getPlatformStoreId());
                log.info("【配送状态消费】步骤1.5: 补充erpStoreCode=platformStoreId={}", message.getPlatformStoreId());
            }
            log.info("【配送状态消费】步骤1.5: 补充完成，merchantCode={}, erpStoreCode={}",
                    message.getMerchantCode(), message.getErpStoreCode());

            log.info("【配送状态消费】步骤2: 查询本地数据库获取旧状态，orderId={}...", orderId);
            OrderDetailRespDTO existingOrder = eleOrderService.getOrderDetail(
                    null, message.getMerchantCode(), message.getErpStoreCode(), orderId);
            Integer oldDeliveryStatus = existingOrder != null ? existingOrder.getDeliveryStatus() : null;
            log.info("【配送状态消费】步骤2: 本地旧配送状态={}，orderId={}", oldDeliveryStatus, orderId);

            log.info("【配送状态消费】步骤3: 调用翱象API拉取最新订单详情，orderId={}...", orderId);
            OrderDetailRespDTO orderDetail = fetchOrderDetailWithRetry(message);
            if (orderDetail == null) {
                log.error("【配送状态消费】步骤3: 翱象API返回订单详情为空，orderId={}", orderId);
                throw new RuntimeException("订单详情不存在，orderId=" + orderId);
            }
            log.info("【配送状态消费】步骤3: 翱象API返回成功 ✓，orderId={}, deliveryStatus={}",
                    orderDetail.getOrderId(), orderDetail.getDeliveryStatus());

            log.info("【配送状态消费】步骤4: 更新订单入库，orderId={}, oldDeliveryStatus={} -> newDeliveryStatus={}...",
                    orderId, oldDeliveryStatus, orderDetail.getDeliveryStatus());
            OrderMessage orderMessage = convertToOrderMessage(orderDetail, message);
            eleOrderService.consumeOrderMessage(orderMessage, true);
            log.info("【配送状态消费】步骤4: 订单更新入库成功 ✓，orderId={}", orderId);

            log.info("【配送状态消费】步骤5: 推送WebSocket通知，orderId={}, oldDeliveryStatus={}, newDeliveryStatus={}...",
                    orderId, oldDeliveryStatus, orderDetail.getDeliveryStatus());
            orderStatusPushService.pushOrderStatusChange(
                    orderId,
                    existingOrder != null ? existingOrder.getStatus() : null,
                    orderDetail.getStatus(),
                    orderDetail.getChannelSourceName(),
                    orderDetail.getBuyerName());
            log.info("【配送状态消费】步骤5: WebSocket推送完成 ✓");

            acknowledgment.acknowledge();
            log.info("【配送状态消费】==== 配送状态变更处理完成 END，orderId={}, deliveryStatus={} ====",
                    orderId, orderDetail.getDeliveryStatus());

        } catch (Exception e) {
            log.error("【配送状态消费】消费失败，orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private void consumeSupplyChainMessage(
            OrderStatusPushMessage message,
            int partition,
            long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        log.info("【供应链消息消费】收到供应链订单消息，orderId={}, ticket={}, partition={}, offset={}",
                orderId, ticket, partition, offset);
        log.info("【供应链消息消费】供应链消息目前仅记录，不做入库处理");

        acknowledgment.acknowledge();
    }

    private void consumeReverseOrderStatusChangeMessage(
            OrderStatusPushMessage message,
            int partition,
            long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        String ticket = message.getTicket();
        log.info("【逆向订单状态消费】==== 开始处理逆向订单状态变更 ====");
        log.info("【逆向订单状态消费】orderId={}, status={}, ticket={}, partition={}, offset={}",
                orderId, message.getStatus(), ticket, partition, offset);

        log.info("【逆向订单状态消费】步骤1: 幂等检查，ticket={}", ticket);
        if (!checkIdempotent(ticket)) {
            log.info("【逆向订单状态消费】步骤1: 幂等检查命中，跳过处理，orderId={}, ticket={}", orderId, ticket);
            acknowledgment.acknowledge();
            return;
        }
        log.info("【逆向订单状态消费】步骤1: 幂等检查通过 ✓");

        try {
            log.info("【逆向订单状态消费】步骤1.5: 补充merchantCode/erpStoreCode...");
            if (StrUtil.isBlank(message.getMerchantCode())) {
                EleApiConfig config = eleApiConfigMapper.selectActive();
                if (config != null) {
                    message.setMerchantCode(config.getMerchantCode());
                    log.info("【逆向订单状态消费】步骤1.5: 从ele_api_config表补充merchantCode={}", config.getMerchantCode());
                } else {
                    log.warn("【逆向订单状态消费】步骤1.5: ele_api_config表无启用记录，merchantCode仍为空");
                }
            }
            if (StrUtil.isBlank(message.getErpStoreCode()) && message.getPlatformStoreId() != null) {
                message.setErpStoreCode(message.getPlatformStoreId());
                log.info("【逆向订单状态消费】步骤1.5: 补充erpStoreCode=platformStoreId={}", message.getPlatformStoreId());
            }
            log.info("【逆向订单状态消费】步骤1.5: 补充完成，merchantCode={}, erpStoreCode={}",
                    message.getMerchantCode(), message.getErpStoreCode());

            log.info("【逆向订单状态消费】步骤2: 调用翱象API拉取最新订单详情，orderId={}...", orderId);
            OrderDetailRespDTO orderDetail = fetchOrderDetailWithRetry(message);
            if (orderDetail == null) {
                log.error("【逆向订单状态消费】步骤2: 翱象API返回订单详情为空，orderId={}", orderId);
                throw new RuntimeException("订单详情不存在，orderId=" + orderId);
            }
            log.info("【逆向订单状态消费】步骤2: 翱象API返回成功 ✓，orderId={}, status={}",
                    orderDetail.getOrderId(), orderDetail.getStatus());

            log.info("【逆向订单状态消费】步骤3: 更新订单入库，orderId={}, status={}...",
                    orderId, orderDetail.getStatus());
            OrderMessage orderMessage = convertToOrderMessage(orderDetail, message);
            eleOrderService.consumeOrderMessage(orderMessage, true);
            log.info("【逆向订单状态消费】步骤3: 订单更新入库成功 ✓，orderId={}", orderId);

            log.info("【逆向订单状态消费】步骤4: 推送WebSocket通知，orderId={}, status={}...",
                    orderId, orderDetail.getStatus());
            orderStatusPushService.pushOrderStatusChange(
                    orderId,
                    null,
                    orderDetail.getStatus(),
                    orderDetail.getChannelSourceName(),
                    orderDetail.getBuyerName());
            log.info("【逆向订单状态消费】步骤4: WebSocket推送完成 ✓");

            acknowledgment.acknowledge();
            log.info("【逆向订单状态消费】==== 逆向订单状态变更处理完成 END，orderId={}, status={} ====",
                    orderId, orderDetail.getStatus());

        } catch (Exception e) {
            log.error("【逆向订单状态消费】消费失败，orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private boolean checkIdempotent(String ticket) {
        try {
            String key = idempotentKeyPrefix + ticket;
            RBucket<String> bucket = redissonClient.getBucket(key);
            Boolean result = bucket.setIfAbsent("1", Duration.ofSeconds(expireSeconds));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("【SaaS推送消费】Redis幂等检查异常，降级继续处理，error={}", e.getMessage());
            return true;
        }
    }

    private OrderDetailRespDTO fetchOrderDetailWithRetry(OrderStatusPushMessage message) {
        Exception lastException = null;
        for (int i = 0; i < maxRetryAttempts; i++) {
            try {
                return fetchOrderDetail(message);
            } catch (Exception e) {
                lastException = e;
                log.warn("【SaaS推送消费】拉取订单详情失败，orderId={}, retry={}/{}, error={}",
                        message.getOrderId(), i + 1, maxRetryAttempts, e.getMessage());
                if (i < maxRetryAttempts - 1) {
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("拉取订单详情失败，orderId=" + message.getOrderId(), lastException);
    }

    private OrderDetailRespDTO fetchOrderDetail(OrderStatusPushMessage message) {
        SaasOrderGetParam.SaasOrderGetBody body = new SaasOrderGetParam.SaasOrderGetBody();
        body.setOrder_id(message.getOrderId());
        body.setMerchant_code(message.getMerchantCode());
        body.setErp_store_code(message.getErpStoreCode());

        SaasOrderGetParam param = new SaasOrderGetParam();
        param.setTicket(UUID.randomUUID().toString().toUpperCase());
        param.setEncrypt("aes");
        param.setBody(body);

        EleApiConfig config = eleApiConfigMapper.selectActive();
        if (config == null) {
            throw new RuntimeException("ele_api_config表无启用记录，无法拉取订单详情，orderId=" + message.getOrderId());
        }

        BizResultWrapper<SaasOrderGetResult> wrapper = eleOpenApiClient.sendOrderDetail(
                config, param, message.getMerchantCode(), null, message.getErpStoreCode(), message.getOrderId());

        if (wrapper == null || wrapper.getBody() == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        SaasOrderGetResult result = wrapper.getBody();
        String errno = result.getErrno();
        if (errno != null && !"0".equals(errno)) {
            throw new RuntimeException("翱象接口返回错误[" + errno + "]: " + result.getError());
        }

        return convertToOrderDetail(result.getData());
    }

    private OrderDetailRespDTO convertToOrderDetail(SaasOrderGetResult.SaasOrderGetData data) {
        OrderDetailRespDTO detail = new OrderDetailRespDTO();
        detail.setOrderId(data.getOrder_id());
        detail.setStatus(data.getStatus());
        detail.setCreateTime(data.getCreate_time());
        detail.setPayTime(data.getPay_time());
        detail.setChannelSourceName(data.getChannel_source_name());
        detail.setBuyerName(data.getBuyer_name());
        detail.setBuyerPhone(data.getBuyer_phone());
        detail.setBuyerAddress(data.getBuyer_address());
        detail.setDeliveryName(data.getDelivery_name());
        detail.setDeliveryPhone(data.getDelivery_phone());
        detail.setDeliveryPlatform(data.getDelivery_platform());
        detail.setDeliveryType(data.getDelivery_type());
        detail.setDeliveryStatus(data.getDelivery_status());
        detail.setTotalFee(data.getTotal_fee());
        detail.setPayFee(data.getPay_fee());
        detail.setDiscountFee(data.getDiscount_fee());
        detail.setDeliveryFee(data.getDelivery_fee());
        detail.setPostFee(data.getPost_fee());
        detail.setPackageFee(data.getPackage_fee());
        detail.setPlatformCommissionFee(data.getPlatform_commission_fee());
        detail.setRemark(data.getRemark());
        detail.setChannelSourceId(data.getChannel_source_id());
        detail.setChannelOrderId(data.getChannel_order_id());
        detail.setChannelType(data.getChannel_type());
        detail.setStoreCode(data.getStore_code());
        detail.setErpStoreCode(data.getErp_store_code());
        detail.setLongitude(data.getLongitude());
        detail.setLatitude(data.getLatitude());
        detail.setArriveType(data.getArrive_type());
        return detail;
    }

    private OrderMessage convertToOrderMessage(OrderDetailRespDTO detail, OrderStatusPushMessage pushMessage) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(detail.getOrderId());
        message.setPlatformStoreId(detail.getStoreCode());
        message.setMerchantCode(pushMessage.getMerchantCode());
        message.setErpStoreCode(pushMessage.getErpStoreCode());
        message.setStatus(detail.getStatus());
        message.setCreateTime(detail.getCreateTime());
        message.setPayTime(detail.getPayTime());
        message.setChannelSourceName(detail.getChannelSourceName());
        message.setBuyerName(detail.getBuyerName());
        message.setBuyerPhone(detail.getBuyerPhone());
        message.setBuyerAddress(detail.getBuyerAddress());
        message.setDeliveryName(detail.getDeliveryName());
        message.setDeliveryPhone(detail.getDeliveryPhone());
        message.setDeliveryPlatform(detail.getDeliveryPlatform());
        message.setDeliveryType(detail.getDeliveryType());
        message.setDeliveryStatus(detail.getDeliveryStatus());
        message.setTotalFee(detail.getTotalFee());
        message.setPayFee(detail.getPayFee());
        message.setDiscountFee(detail.getDiscountFee());
        message.setDeliveryFee(detail.getDeliveryFee());
        message.setPostFee(detail.getPostFee());
        message.setPackageFee(detail.getPackageFee());
        message.setPlatformCommissionFee(detail.getPlatformCommissionFee());
        message.setRemark(detail.getRemark());
        message.setChannelSourceId(detail.getChannelSourceId());
        message.setChannelOrderId(detail.getChannelOrderId());
        message.setChannelType(detail.getChannelType());
        message.setStoreCode(detail.getStoreCode());
        message.setLongitude(detail.getLongitude());
        message.setLatitude(detail.getLatitude());
        message.setArriveType(detail.getArriveType());
        message.setRealtime(Boolean.TRUE);
        message.setMessageTime(System.currentTimeMillis());
        message.setRetryCount(0);
        return message;
    }
}
