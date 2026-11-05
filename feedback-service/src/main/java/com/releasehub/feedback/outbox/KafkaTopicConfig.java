package com.releasehub.feedback.outbox;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.List;

@Configuration
public class KafkaTopicConfig {

    @Bean
    List<NewTopic> releaseHubTopics() {
        return List.of(
                topic("feedback.submitted", 3),
                topic("download.completed", 3),
                topic("feedback.submitted.dlq", 1),
                topic("download.completed.dlq", 1)
        );
    }

    private NewTopic topic(String name, int partitions) {
        return TopicBuilder.name(name).partitions(partitions).replicas(1).build();
    }
}
