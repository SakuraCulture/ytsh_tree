package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.dto.OrderRetryMessage;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class EleOrderRetryKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(EleOrderRetryKafkaProducer.class);

    @Resource
    @Qualifier("eleOrderRetryKafkaTemplate")
    private KafkaTemplate<String, OrderRetryMessage> kafkaTemplate;

    @Value("${ele.kafka.retry.topic:ele-order-retry}")
    private String retryTopic;

    @Value("${ele.kafka.retry.dlq-topic:ele-order-retry-dlq}")
    private String dlqTopic;

    public CompletableFuture<SendResult<String, OrderRetryMessage>> sendRetryMessage(OrderRetryMessage message) {
        String orderId = message.getOrderId();
        log.info("【重试Kafka】发送重试消息到topic={}, orderId={}, retryCount={}",
                retryTopic, orderId, message.getRetryCount());

        CompletableFuture<SendResult<String, OrderRetryMessage>> future = kafkaTemplate.send(retryTopic, orderId, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("【重试Kafka】重试消息发送成功，orderId={}, partition={}, offset={}",
                        orderId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("【重试Kafka】重试消息发送失败，orderId={}, error={}", orderId, ex.getMessage());
            }
        });

        return future;
    }

    public boolean sendRetryMessageAndWait(OrderRetryMessage message) {
        String orderId = message.getOrderId();
        log.info("【重试Kafka】发送重试消息到topic={}, orderId={}, retryCount={}",
                retryTopic, orderId, message.getRetryCount());

        try {
            CompletableFuture<SendResult<String, OrderRetryMessage>> future = kafkaTemplate.send(retryTopic, orderId, message);
            SendResult<String, OrderRetryMessage> result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            log.info("【重试Kafka】重试消息发送成功，orderId={}, partition={}, offset={}",
                    orderId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return true;
        } catch (Exception ex) {
            log.error("【重试Kafka】重试消息发送失败，orderId={}, error={}", orderId, ex.getMessage());
            return false;
        }
    }

    public CompletableFuture<SendResult<String, OrderRetryMessage>> sendDeadLetterMessage(OrderRetryMessage message) {
        String orderId = message.getOrderId();
        log.warn("【死信Kafka】发送死信消息到topic={}, orderId={}, retryCount={}",
                dlqTopic, orderId, message.getRetryCount());

        CompletableFuture<SendResult<String, OrderRetryMessage>> future = kafkaTemplate.send(dlqTopic, orderId, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("【死信Kafka】死信消息发送成功，orderId={}, partition={}, offset={}",
                        orderId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("【死信Kafka】死信消息发送失败，orderId={}, error={}", orderId, ex.getMessage());
            }
        });

        return future;
    }
}
