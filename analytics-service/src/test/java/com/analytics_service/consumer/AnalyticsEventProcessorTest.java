package com.analytics_service.consumer;

import com.analytics_service.event.FeedbackSubmittedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventProcessorTest {

    private static final String REGISTER_EVENT_SQL = """
            INSERT IGNORE INTO processed_event (event_id, event_type, processed_at)
            VALUES (?, ?, CURRENT_TIMESTAMP(6))
            """;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AnalyticsEventProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new AnalyticsEventProcessor(jdbcTemplate, objectMapper);
    }

    @Test
    void feedbackEventsAreRegisteredAndAppliedToAggregate() throws Exception {
        String feedbackId = UUID.randomUUID().toString();
        String softwareId = UUID.randomUUID().toString();
        String payload = objectMapper.writeValueAsString(
                new FeedbackSubmittedEvent(feedbackId, UUID.randomUUID().toString(), softwareId, UUID.randomUUID().toString(), 4));
        when(jdbcTemplate.update(REGISTER_EVENT_SQL, feedbackId, "feedback.submitted")).thenReturn(1);

        processor.processFeedback(payload);

        verify(jdbcTemplate).update(argThat(sql -> sql.contains("INSERT INTO report_aggregate")),
                eq(softwareId), eq(4), eq(4));
    }

    @Test
    void duplicateFeedbackEventsDoNotUpdateTheAggregateAgain() throws Exception {
        String feedbackId = UUID.randomUUID().toString();
        String payload = objectMapper.writeValueAsString(
                new FeedbackSubmittedEvent(feedbackId, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), 4));
        when(jdbcTemplate.update(REGISTER_EVENT_SQL, feedbackId, "feedback.submitted")).thenReturn(0);

        processor.processFeedback(payload);

        verify(jdbcTemplate, never()).update(argThat(sql -> sql.contains("INSERT INTO report_aggregate")), any(Object[].class));
    }

    @Test
    void invalidPayloadFailsSoKafkaCanRetryAndDeadLetterIt() {
        assertThatThrownBy(() -> processor.processFeedback("{malformed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid JSON");
    }
}
