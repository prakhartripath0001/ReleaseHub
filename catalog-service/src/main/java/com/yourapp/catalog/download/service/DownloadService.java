package com.yourapp.catalog.download.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.download.dto.response.DownloadResponse;
import com.yourapp.catalog.download.entity.DownloadEventEntity;
import com.yourapp.catalog.download.event.DownloadCompletedEvent;
import com.yourapp.catalog.download.repository.DownloadEventRepository;
import com.yourapp.catalog.outbox.entity.OutboxEventEntity;
import com.yourapp.catalog.outbox.repository.OutboxEventRepository;
import com.yourapp.catalog.version.entity.VersionEntity;
import com.yourapp.catalog.version.repository.VersionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final VersionRepository versionRepository;
    private final DownloadEventRepository downloadEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public DownloadResponse recordAndResolve(String versionId, String userId) {
        VersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

        DownloadEventEntity downloadEvent = downloadEventRepository.save(
                DownloadEventEntity.builder()
                        .versionId(versionId)
                        .userId(userId)
                        .build()
        );
        DownloadCompletedEvent event = new DownloadCompletedEvent(
                downloadEvent.getId(),
                versionId,
                version.getSoftware().getId(),
                userId,
                downloadEvent.getTs());
        try {
            outboxEventRepository.save(new OutboxEventEntity(
                    event.softwareId(),
                    "download.completed",
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize download event for " + versionId, ex);
        }
        Counter.builder("releasehub.download.events")
                .description("Successfully recorded download events")
                .register(meterRegistry)
                .increment();

        DownloadResponse response = new DownloadResponse();
        response.setVersionId(versionId);
        response.setDownloadUrl(
                version.getFileRef() != null ? version.getFileRef() : "/files/" + versionId
        );
        response.setMessage("Download recorded successfully");
        return response;
    }
}
