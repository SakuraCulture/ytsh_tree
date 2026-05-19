package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.EleOrderService;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EleOrderKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EleOrderKafkaConsumer.class);
    private static final String REALTIME_LOCK_PREFIX = "ele:order:realtime:";

    @Resource
    private EleOrderService eleOrderService;

    @Resource
    private RedissonClient redissonClient;

    @KafkaListener(
            topics = "${ele.kafka.topic.realtime:ele-order-realtime}",
            groupId = "${ele.kafka.consumer.group-id:ele-order-realtime-consumer}",
            containerFactory = "eleOrderKafkaListenerContainerFactory"
    )
    public void consumeOrderMessage(
            @Payload OrderMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        String orderId = message.getOrderId();
        log.info("【实时Kafka】消费订单消息，orderId={}, partition={}, offset={}", orderId, partition, offset);

        String lockKey = REALTIME_LOCK_PREFIX + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.info("【实时消费-去重】订单正在被其他Consumer处理，跳过，orderId={}", orderId);
                acknowledgment.acknowledge();
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【实时消费】获取锁被中断，orderId={}", orderId);
            acknowledgment.acknowledge();
            return;
        }

        try {
            eleOrderService.consumeOrderMessage(message);
            acknowledgment.acknowledge();
            log.info("【实时Kafka】订单消费成功，orderId={}", orderId);
        } catch (Exception e) {
            log.error("【实时Kafka】订单消费异常，orderId={}, error={}", orderId, e.getMessage());
            throw e;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}