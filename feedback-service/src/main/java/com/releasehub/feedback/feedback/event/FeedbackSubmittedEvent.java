package com.releasehub.feedback.feedback.event;

import java.time.LocalDateTime;

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
