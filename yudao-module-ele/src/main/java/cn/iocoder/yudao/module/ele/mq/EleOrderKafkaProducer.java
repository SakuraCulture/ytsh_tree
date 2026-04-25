package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class EleOrderKafkaProducer {

    @Autowired
    private KafkaTemplate<String, OrderMessage> kafkaTemplate;

    @Value("${ele.kafka.topic.realtime:ele-order-realtime}")
    private String topic;

    public CompletableFuture<SendResult<String, OrderMessage>> sendOrderMessage(OrderMessage message) {
        String orderId = message.getOrderId();
        CompletableFuture<SendResult<String, OrderMessage>> future = kafkaTemplate.send(topic, orderId, message);
        return future;
    }
}