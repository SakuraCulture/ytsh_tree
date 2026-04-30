package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.dto.OrderStatusPushMessage;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class SaasOrderStatusPushProducer {

    @Resource
    @Qualifier("saasPushKafkaTemplate")
    private KafkaTemplate<String, OrderStatusPushMessage> kafkaTemplate;

    @Value("${ele.saas.push.kafka.topic:order-status-change}")
    private String topic;

    public CompletableFuture<SendResult<String, OrderStatusPushMessage>> sendPushMessage(OrderStatusPushMessage message) {
        String orderId = message.getOrderId();
        return kafkaTemplate.send(topic, orderId, message);
    }
}
