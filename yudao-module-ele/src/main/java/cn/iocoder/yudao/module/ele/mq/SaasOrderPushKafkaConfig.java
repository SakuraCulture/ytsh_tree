package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.dto.OrderStatusPushMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
@Lazy(false)
public class SaasOrderPushKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String jaasConfig;

    @Value("${spring.kafka.properties.sasl.mechanism:PLAIN}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.security.protocol:SASL_PLAINTEXT}")
    private String securityProtocol;

    @Value("${ele.saas.push.enabled:false}")
    private boolean pushEnabled;

    @Bean
    public ProducerFactory<String, OrderStatusPushMessage> saasPushProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        if (!jaasConfig.isEmpty()) {
            props.put("sasl.mechanism", saslMechanism);
            props.put("security.protocol", securityProtocol);
            props.put("sasl.jaas.config", jaasConfig);
        }
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, OrderStatusPushMessage> saasPushKafkaTemplate() {
        return new KafkaTemplate<>(saasPushProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, OrderStatusPushMessage> saasPushConsumerFactory(
            @Value("${ele.saas.push.kafka.consumer-group-id:order-status-consumer}") String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        if (!jaasConfig.isEmpty()) {
            props.put("sasl.mechanism", saslMechanism);
            props.put("security.protocol", securityProtocol);
            props.put("sasl.jaas.config", jaasConfig);
        }

        JsonDeserializer<OrderStatusPushMessage> deserializer = new JsonDeserializer<>(OrderStatusPushMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean("saasPushKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderStatusPushMessage> saasPushKafkaListenerContainerFactory(
            @Value("${ele.saas.push.kafka.consumer-group-id:order-status-consumer}") String groupId,
            @Value("${ele.saas.push.kafka.topic:order-status-change}") String topic) {
        ConcurrentKafkaListenerContainerFactory<String, OrderStatusPushMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(saasPushConsumerFactory(groupId));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(60000L);
        backOff.setMaxElapsedTime(300000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> log.error("【SaaS推送Kafka】消费失败，record={}, error={}", record, ex.getMessage(), ex),
                backOff);
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> log.warn(
                "【SaaS推送Kafka重试】第{}次重试失败，topic={}, partition={}, offset={}, error={}",
                deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage()));
        factory.setCommonErrorHandler(errorHandler);

        log.info("【SaaS推送Kafka】消费者容器工厂初始化完成，topic={}, groupId={}, pushEnabled={}", topic, groupId, pushEnabled);
        return factory;
    }
}
