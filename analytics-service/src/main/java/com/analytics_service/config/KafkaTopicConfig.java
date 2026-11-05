package com.analytics_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic feedbackSubmittedTopic() {
        return topic("feedback.submitted", 3);
    }

    @Bean
    NewTopic downloadCompletedTopic() {
        return topic("download.completed", 3);
    }

    @Bean
    NewTopic feedbackDeadLetterTopic() {
        return topic("feedback.submitted.dlq", 1);
    }

    @Bean
    NewTopic downloadDeadLetterTopic() {
        return topic("download.completed.dlq", 1);
    }

    private NewTopic topic(String name, int partitions) {
        return TopicBuilder.name(name).partitions(partitions).replicas(1).build();
    }
}
