package cn.iocoder.yudao.module.ele.mq;

import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;
import cn.iocoder.yudao.module.ele.service.dto.OrderRetryMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
public class EleOrderKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String jaasConfig;

    @Value("${spring.kafka.properties.sasl.mechanism:PLAIN}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.security.protocol:SASL_PLAINTEXT}")
    private String securityProtocol;

    private Map<String, Object> buildCommonProducerProps() {
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
        return props;
    }

    private Map<String, Object> buildCommonConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        if (!jaasConfig.isEmpty()) {
            props.put("sasl.mechanism", saslMechanism);
            props.put("security.protocol", securityProtocol);
            props.put("sasl.jaas.config", jaasConfig);
        }
        return props;
    }

    private DefaultErrorHandler createErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(
                1000L,
                2.0);
        backOff.setMaxInterval(60000L);
        backOff.setMaxElapsedTime(300000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> log.error("【Kafka消费】消费失败，record={}, error={}", record, ex.getMessage(), ex),
                backOff);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("【Kafka重试】第{}次重试失败，topic={}, partition={}, offset={}, error={}",
                        deliveryAttempt,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        ex.getMessage())
        );

        return errorHandler;
    }

    @Bean
    @Primary
    public ProducerFactory<Object, Object> kafkaProducerFactory() {
        return new DefaultKafkaProducerFactory<>(buildCommonProducerProps());
    }

    @Bean
    @Primary
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(kafkaProducerFactory());
    }

    @Bean
    public ConsumerFactory<Object, Object> kafkaConsumerFactory() {
        Map<String, Object> props = buildCommonConsumerProps("default-kafka-group");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ProducerFactory<String, OrderMessage> eleOrderProducerFactory() {
        return new DefaultKafkaProducerFactory<>(buildCommonProducerProps());
    }

    @Bean
    public KafkaTemplate<String, OrderMessage> eleOrderKafkaTemplate() {
        return new KafkaTemplate<>(eleOrderProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, OrderMessage> eleOrderConsumerFactory(
            @Value("${ele.kafka.consumer.group-id:ele-order-realtime-consumer}") String groupId) {
        Map<String, Object> props = buildCommonConsumerProps(groupId);
        JsonDeserializer<OrderMessage> deserializer = new JsonDeserializer<>(OrderMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        deserializer.setTypeMapper(new org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper() {
            {
                setTypePrecedence(
                        org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
            }
        });
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean("eleOrderKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderMessage> eleOrderKafkaListenerContainerFactory(
            @Value("${ele.kafka.consumer.group-id:ele-order-realtime-consumer}") String groupId) {
        ConcurrentKafkaListenerContainerFactory<String, OrderMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eleOrderConsumerFactory(groupId));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(createErrorHandler());
        return factory;
    }

    @Bean
    public ProducerFactory<String, OrderRetryMessage> eleOrderRetryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(buildCommonProducerProps());
    }

    @Bean
    public KafkaTemplate<String, OrderRetryMessage> eleOrderRetryKafkaTemplate() {
        return new KafkaTemplate<>(eleOrderRetryProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, OrderRetryMessage> eleOrderRetryConsumerFactory(
            @Value("${ele.kafka.retry.consumer.group-id:ele-order-retry-consumer}") String groupId) {
        Map<String, Object> props = buildCommonConsumerProps(groupId);
        JsonDeserializer<OrderRetryMessage> deserializer = new JsonDeserializer<>(OrderRetryMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        deserializer.setTypeMapper(new org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper() {
            {
                setTypePrecedence(
                        org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
            }
        });
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean("eleOrderRetryKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderRetryMessage> eleOrderRetryKafkaListenerContainerFactory(
            @Value("${ele.kafka.retry.consumer.group-id:ele-order-retry-consumer}") String groupId,
            @Value("${ele.kafka.retry.consumer.concurrency:5}") int concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, OrderRetryMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eleOrderRetryConsumerFactory(groupId));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(createErrorHandler());
        factory.setConcurrency(concurrency);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrderRetryMessage> eleOrderRetryDlqConsumerFactory(
            @Value("${ele.kafka.retry.consumer.dlq-group-id:ele-order-retry-dlq-consumer}") String groupId) {
        Map<String, Object> props = buildCommonConsumerProps(groupId);
        JsonDeserializer<OrderRetryMessage> deserializer = new JsonDeserializer<>(OrderRetryMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        deserializer.setTypeMapper(new org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper() {
            {
                setTypePrecedence(
                        org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
            }
        });
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean("eleOrderRetryDlqKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderRetryMessage> eleOrderRetryDlqKafkaListenerContainerFactory(
            @Value("${ele.kafka.retry.consumer.dlq-group-id:ele-order-retry-dlq-consumer}") String groupId) {
        ConcurrentKafkaListenerContainerFactory<String, OrderRetryMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eleOrderRetryDlqConsumerFactory(groupId));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(createErrorHandler());
        factory.setConcurrency(1);
        return factory;
    }
}
