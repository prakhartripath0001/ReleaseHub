package com.analytics_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeedbackSubmittedEvent(
        String feedbackId,
        String versionId,
        String softwareId,
        String userId,
        int rating,
        String comment,
        LocalDateTime occurredAt
) {
}
