package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import jakarta.annotation.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EleOrderKafkaConsumer {

    @Resource
    private EleOrderService eleOrderService;

    @KafkaListener(
            topics = "${ele.kafka.topic.realtime:ele-order-realtime}",
            groupId = "${ele.kafka.consumer.group-id:ele-order-realtime-consumer}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderMessage(
            @Payload OrderMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        try {
            eleOrderService.consumeOrderMessage(message);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}