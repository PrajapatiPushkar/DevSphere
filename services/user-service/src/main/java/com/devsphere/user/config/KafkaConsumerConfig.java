package com.devsphere.user.config;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${app.kafka.consumer.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.kafka.consumer.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<Object, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.warn("Routing message to DLT topic: {} [partition: {}, offset: {}] after retry exhaustion. Cause: {}",
                            dltTopic, record.partition(), record.offset(),
                            exception != null ? exception.getMessage() : "Unknown");
                    return new TopicPartition(dltTopic, record.partition());
                }
        );

        long retryCount = Math.max(0, maxAttempts - 1L);
        FixedBackOff backOff = new FixedBackOff(retryBackoffMs, retryCount);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Kafka processing retry attempt {}/{} for topic: {}, partition: {}, offset: {}, key: {}. Error: {}",
                    deliveryAttempt, maxAttempts, record.topic(), record.partition(), record.offset(), record.key(), ex.getMessage());
        });

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
