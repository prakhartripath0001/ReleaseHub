package com.yourapp.catalog.download.event;

import java.time.LocalDateTime;

public record DownloadCompletedEvent(
        String downloadId,
        String versionId,
        String softwareId,
        String userId,
        LocalDateTime occurredAt
) {
}
