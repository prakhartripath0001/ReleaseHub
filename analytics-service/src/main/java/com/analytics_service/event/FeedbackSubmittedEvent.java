package com.analytics_service.event;

public record FeedbackSubmittedEvent(
        String feedbackId,
        String versionId,
        String softwareId,
        String userId,
        int rating
) {
}
