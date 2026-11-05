package com.analytics_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DownloadCompletedEvent(
        String downloadId,
        String versionId,
        String softwareId,
        String userId,
        LocalDateTime occurredAt
) {
}
