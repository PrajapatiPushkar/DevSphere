package com.devsphere.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String DOMAIN_EVENTS_TOPIC = "devsphere.domain.events";
    public static final String DOMAIN_EVENTS_DLT = "devsphere.domain.events.DLT";

    @Bean
    public NewTopic domainEventsTopic() {
        return TopicBuilder.name(DOMAIN_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic domainEventsDltTopic() {
        return TopicBuilder.name(DOMAIN_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
