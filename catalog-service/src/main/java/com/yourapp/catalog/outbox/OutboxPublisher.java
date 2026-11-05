package com.yourapp.catalog.outbox;

import com.yourapp.catalog.outbox.entity.OutboxEventEntity;
import com.yourapp.catalog.outbox.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${releasehub.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEventEntity> events = repository.findTop20ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEventEntity event : events) {
            try {
                RecordMetadata metadata = kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .getRecordMetadata();
                event.setPublishedAt(LocalDateTime.now());
                log.info("Published outbox event {} to {} partition {} offset {}",
                        event.getId(), event.getEventType(), metadata.partition(), metadata.offset());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while publishing outbox event " + event.getId(), ex);
            } catch (ExecutionException | TimeoutException ex) {
                throw new IllegalStateException("Could not publish outbox event " + event.getId(), ex);
            }
        }
    }
}
