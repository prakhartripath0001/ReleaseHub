package com.analytics_service.event;

public record DownloadCompletedEvent(
        String downloadId,
        String versionId,
        String softwareId,
        String userId
) {
}
