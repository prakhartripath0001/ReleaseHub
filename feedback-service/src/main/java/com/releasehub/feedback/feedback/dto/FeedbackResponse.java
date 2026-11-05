package com.releasehub.feedback.feedback.dto;

import java.time.LocalDateTime;

public record FeedbackResponse(
        String id,
        String versionId,
        String softwareId,
        String userId,
        int rating,
        String comment,
        LocalDateTime createdAt
) {
}
