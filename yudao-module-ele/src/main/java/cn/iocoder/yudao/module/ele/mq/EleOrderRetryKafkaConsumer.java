package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderFailRecordMapper;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import cn.iocoder.yudao.module.ele.service.dto.OrderRetryMessage;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EleOrderRetryKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EleOrderRetryKafkaConsumer.class);

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private EleOrderFailRecordMapper eleOrderFailRecordMapper;

    @Resource
    private EleOrderRetryKafkaProducer retryKafkaProducer;

    @Resource
    private RedissonClient redissonClient;

    @Value("${ele.kafka.retry.max-retry-count:3}")
    private int maxRetryCount;

    private static final String RETRY_LOCK_PREFIX = "ele:order:retry:";

    @KafkaListener(topics = "${ele.kafka.retry.topic:ele-order-retry}", groupId = "${ele.kafka.retry.consumer.group-id:ele-order-retry-consumer}", concurrency = "${ele.kafka.retry.consumer.concurrency:5}")
    public void consumeRetryMessage(
            @Payload OrderRetryMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        log.info("【重试Kafka】消费重试消息，orderId={}, partition={}, offset={}, retryCount={}",
                orderId, partition, offset, message.getRetryCount());

        // 分布式锁去重: 防止同一订单被多个Consumer同时处理
        String lockKey = RETRY_LOCK_PREFIX + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.info("【重试消费-去重】订单正在被其他Consumer处理，跳过，orderId={}", orderId);
                acknowledgment.acknowledge();
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【重试消费】获取锁被中断，orderId={}", orderId);
            acknowledgment.acknowledge();
            return;
        }

        try {
            OrderListRespDTO.OrderDetail orderDetail = queryOrderDetail(message);
            if (orderDetail == null) {
                throw new RuntimeException("订单详情不存在，无法重试，orderId=" + orderId);
            }

            OrderMessage orderMessage = buildOrderMessage(orderDetail, message);
            eleOrderService.consumeOrderMessage(orderMessage);

            EleOrderFailRecord record = eleOrderFailRecordMapper.selectById(message.getFailRecordId());
            if (record != null) {
                record.setProcessStatus("SUCCESS");
                record.setUpdateTime(System.currentTimeMillis());
                if (StrUtil.isBlank(record.getRemark())) {
                    record.setRemark("Kafka重试成功，自动清理");
                } else {
                    record.setRemark(record.getRemark() + " | Kafka重试成功，自动清理");
                }
                eleOrderFailRecordMapper.updateById(record);
                log.info("【重试Kafka】失败记录标记成功，orderId={}, failRecordId={}", orderId, message.getFailRecordId());
            }
            acknowledgment.acknowledge();
            log.info("【重试Kafka】订单重试成功，orderId={}", orderId);

        } catch (Exception e) {
            log.error("【重试Kafka】订单重试失败，orderId={}, error={}", orderId, e.getMessage());
            handleRetryFailure(message, e);
            acknowledgment.acknowledge();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @KafkaListener(topics = "${ele.kafka.retry.dlq-topic:ele-order-retry-dlq}", groupId = "${ele.kafka.retry.consumer.dlq-group-id:ele-order-retry-dlq-consumer}", concurrency = "1")
    public void consumeDeadLetterMessage(
            @Payload OrderRetryMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        log.warn("【死信Kafka】消费死信消息，orderId={}, partition={}, offset={}, retryCount={}",
                orderId, partition, offset, message.getRetryCount());

        try {
            updateFailRecordStatus(message.getFailRecordId(), "PENDING_MANUAL",
                    "死信队列：重试" + message.getRetryCount() + "次全部失败，等待人工处理");
            acknowledgment.acknowledge();
            log.info("【死信Kafka】死信消息处理完成，orderId={}", orderId);
        } catch (Exception e) {
            log.error("【死信Kafka】处理死信消息异常，orderId={}, error={}", orderId, e.getMessage());
        }
    }

    private OrderListRespDTO.OrderDetail queryOrderDetail(OrderRetryMessage message) {
        try {
            OrderDetailRespDTO detail = eleOrderService.getOrderDetail(
                    message.getPlatformStoreId(),
                    message.getMerchantCode(),
                    message.getErpStoreCode(),
                    message.getOrderId());
            if (detail == null) {
                return null;
            }
            return convertToOrderListDetail(detail);
        } catch (Exception e) {
            log.warn("【重试Kafka】查询订单详情失败，orderId={}, error={}", message.getOrderId(), e.getMessage());
            return null;
        }
    }

    private OrderListRespDTO.OrderDetail convertToOrderListDetail(OrderDetailRespDTO detail) {
        if (detail == null) {
            return null;
        }
        OrderListRespDTO.OrderDetail orderDetail = new OrderListRespDTO.OrderDetail();
        orderDetail.setOrderId(detail.getOrderId());
        orderDetail.setChannelOrderId(detail.getChannelOrderId());
        orderDetail.setStatus(detail.getStatus());
        orderDetail.setCreateTime(detail.getCreateTime());
        orderDetail.setPayTime(detail.getPayTime());
        orderDetail.setChannelSourceName(detail.getChannelSourceName());
        orderDetail.setBuyerName(detail.getBuyerName());
        orderDetail.setBuyerPhone(detail.getBuyerPhone());
        orderDetail.setBuyerAddress(detail.getBuyerAddress());
        orderDetail.setDeliveryName(detail.getDeliveryName());
        orderDetail.setDeliveryPhone(detail.getDeliveryPhone());
        orderDetail.setDeliveryPlatform(detail.getDeliveryPlatform());
        orderDetail.setDeliveryType(detail.getDeliveryType());
        orderDetail.setDeliveryStatus(detail.getDeliveryStatus());
        orderDetail.setTotalFee(detail.getTotalFee());
        orderDetail.setPayFee(detail.getPayFee());
        orderDetail.setDiscountFee(detail.getDiscountFee());
        orderDetail.setDeliveryFee(detail.getDeliveryFee());
        orderDetail.setPostFee(detail.getPostFee());
        orderDetail.setPackageFee(detail.getPackageFee());
        orderDetail.setPlatformCommissionFee(detail.getPlatformCommissionFee());
        orderDetail.setRemark(detail.getRemark());
        orderDetail.setChannelSourceId(detail.getChannelSourceId());
        orderDetail.setChannelType(detail.getChannelType());
        orderDetail.setStoreCode(detail.getStoreCode());
        orderDetail.setErpStoreCode(detail.getErpStoreCode());
        orderDetail.setLongitude(detail.getLongitude());
        orderDetail.setLatitude(detail.getLatitude());
        orderDetail.setSubOrders(detail.getSubOrders());
        orderDetail.setDiscounts(detail.getDiscounts());
        return orderDetail;
    }

    private OrderMessage buildOrderMessage(OrderListRespDTO.OrderDetail orderDetail, OrderRetryMessage retryMessage) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderDetail.getOrderId());
        message.setChannelOrderId(orderDetail.getChannelOrderId());
        message.setPlatformStoreId(retryMessage.getPlatformStoreId());
        message.setMerchantCode(retryMessage.getMerchantCode());
        message.setErpStoreCode(retryMessage.getErpStoreCode());
        message.setStatus(orderDetail.getStatus());
        message.setCreateTime(orderDetail.getCreateTime());
        message.setPayTime(orderDetail.getPayTime());
        message.setChannelSourceName(orderDetail.getChannelSourceName());
        message.setBuyerName(orderDetail.getBuyerName());
        message.setBuyerPhone(orderDetail.getBuyerPhone());
        message.setBuyerAddress(orderDetail.getBuyerAddress());
        message.setDeliveryName(orderDetail.getDeliveryName());
        message.setDeliveryPhone(orderDetail.getDeliveryPhone());
        message.setDeliveryPlatform(orderDetail.getDeliveryPlatform());
        message.setDeliveryType(orderDetail.getDeliveryType());
        message.setDeliveryStatus(orderDetail.getDeliveryStatus());
        message.setTotalFee(orderDetail.getTotalFee());
        message.setPayFee(orderDetail.getPayFee());
        message.setDiscountFee(orderDetail.getDiscountFee());
        message.setDeliveryFee(orderDetail.getDeliveryFee());
        message.setPostFee(orderDetail.getPostFee());
        message.setPackageFee(orderDetail.getPackageFee());
        message.setPlatformCommissionFee(orderDetail.getPlatformCommissionFee());
        message.setRemark(orderDetail.getRemark());
        message.setChannelSourceId(orderDetail.getChannelSourceId());
        message.setChannelType(orderDetail.getChannelType());
        message.setStoreCode(orderDetail.getStoreCode());
        message.setLongitude(orderDetail.getLongitude());
        message.setLatitude(orderDetail.getLatitude());
        message.setSubOrdersJson(JSONUtil.toJsonStr(orderDetail.getSubOrders()));
        message.setDiscountsJson(JSONUtil.toJsonStr(orderDetail.getDiscounts()));
        return message;
    }

    private void updateFailRecordStatus(Long failRecordId, String status, String errorMessage) {
        if (failRecordId == null) {
            return;
        }
        try {
            EleOrderFailRecord record = eleOrderFailRecordMapper.selectById(failRecordId);
            if (record == null) {
                return;
            }
            record.setProcessStatus(status);
            if (errorMessage != null) {
                record.setFailMessage(errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage);
            }
            record.setUpdateTime(System.currentTimeMillis());
            eleOrderFailRecordMapper.updateById(record);
        } catch (Exception e) {
            log.error("【重试Kafka】更新失败记录状态异常，failRecordId={}, error={}", failRecordId, e.getMessage());
        }
    }

    private void clearFailRecord(Long failRecordId, String orderId) {
        try {
            EleOrderFailRecord record = eleOrderFailRecordMapper.selectById(failRecordId);
            if (record != null) {
                record.setProcessStatus("SUCCESS");
                record.setUpdateTime(System.currentTimeMillis());
                if (StrUtil.isBlank(record.getRemark())) {
                    record.setRemark("Kafka重试成功，自动清理");
                } else {
                    record.setRemark(record.getRemark() + " | Kafka重试成功，自动清理");
                }
                eleOrderFailRecordMapper.updateById(record);
                log.info("【重试Kafka】失败记录标记成功，orderId={}, failRecordId={}", orderId, failRecordId);
            }
        } catch (Exception e) {
            log.error("【重试Kafka】清理失败记录异常，orderId={}, error={}", orderId, e.getMessage());
        }
    }

    private void handleRetryFailure(OrderRetryMessage message, Exception e) {
        int currentRetryCount = message.getRetryCount() == null ? 1 : message.getRetryCount();

        if (currentRetryCount >= maxRetryCount) {
            log.warn("【重试Kafka】订单重试{}次全部失败，退回FAILED状态，orderId={}", currentRetryCount, message.getOrderId());
            updateFailRecordStatus(message.getFailRecordId(), "FAILED",
                    "Kafka重试" + currentRetryCount + "次全部失败，等待人工处理");
        } else {
            message.setRetryCount(currentRetryCount + 1);
            message.setCreateTime(System.currentTimeMillis());
            String traceId = TracerUtils.getTraceId();
            if (StrUtil.isNotBlank(traceId)) {
                message.setTraceId(traceId);
            }
            log.info("【重试Kafka】重新发送重试消息，orderId={}, 新retryCount={}", message.getOrderId(), message.getRetryCount());
            retryKafkaProducer.sendRetryMessage(message);

            EleOrderFailRecord record = eleOrderFailRecordMapper.selectById(message.getFailRecordId());
            if (record != null) {
                record.setProcessStatus("PENDING_RETRY");
                record.setRetryCount(currentRetryCount + 1);
                record.setFailMessage(e.getMessage() != null && e.getMessage().length() > 1000
                        ? e.getMessage().substring(0, 1000)
                        : e.getMessage());
                record.setUpdateTime(System.currentTimeMillis());
                eleOrderFailRecordMapper.updateById(record);
            }
        }
    }
}
