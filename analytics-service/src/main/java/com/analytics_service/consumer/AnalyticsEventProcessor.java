package com.analytics_service.consumer;

import com.analytics_service.event.DownloadCompletedEvent;
import com.analytics_service.event.FeedbackSubmittedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AnalyticsEventProcessor {

    private static final String FEEDBACK_TOPIC = "feedback.submitted";
    private static final String DOWNLOAD_TOPIC = "download.completed";
    private static final String REGISTER_EVENT_SQL = """
            INSERT IGNORE INTO processed_event (event_id, event_type, processed_at)
            VALUES (?, ?, CURRENT_TIMESTAMP(6))
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsEventProcessor(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processFeedback(String payload) {
        FeedbackSubmittedEvent event = read(payload, FeedbackSubmittedEvent.class);
        requireText(event.feedbackId(), "feedbackId");
        requireText(event.softwareId(), "softwareId");
        if (event.rating() < 1 || event.rating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        if (!registerOnce(event.feedbackId(), FEEDBACK_TOPIC)) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO report_aggregate
                    (software_id, downloads, avg_rating, feedback_count, rating_sum, updated_at)
                VALUES (?, 0, ?, 1, ?, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE
                    avg_rating = (rating_sum + VALUES(rating_sum)) /
                                 (feedback_count + VALUES(feedback_count)),
                    rating_sum = rating_sum + VALUES(rating_sum),
                    feedback_count = feedback_count + VALUES(feedback_count),
                    updated_at = VALUES(updated_at)
                """, event.softwareId(), event.rating(), event.rating());
    }

    @Transactional
    public void processDownload(String payload) {
        DownloadCompletedEvent event = read(payload, DownloadCompletedEvent.class);
        requireText(event.downloadId(), "downloadId");
        requireText(event.softwareId(), "softwareId");
        if (!registerOnce(event.downloadId(), DOWNLOAD_TOPIC)) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO report_aggregate
                    (software_id, downloads, avg_rating, feedback_count, rating_sum, updated_at)
                VALUES (?, 1, 0, 0, 0, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE
                    downloads = downloads + 1,
                    updated_at = CURRENT_TIMESTAMP(6)
                """, event.softwareId());
    }

    private boolean registerOnce(String eventId, String eventType) {
        return jdbcTemplate.update(REGISTER_EVENT_SQL, eventId, eventType) == 1;
    }

    private <T> T read(String payload, Class<T> eventType) {
        try {
            return Objects.requireNonNull(objectMapper.readValue(payload, eventType), "Event payload must not be null");
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Kafka event payload is not valid JSON", ex);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
