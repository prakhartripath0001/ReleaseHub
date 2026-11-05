package com.yourapp.catalog.download;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.download.dto.response.DownloadResponse;
import com.yourapp.catalog.download.entity.DownloadEventEntity;
import com.yourapp.catalog.download.repository.DownloadEventRepository;
import com.yourapp.catalog.outbox.entity.OutboxEventEntity;
import com.yourapp.catalog.outbox.repository.OutboxEventRepository;
import com.yourapp.catalog.software.entity.SoftwareEntity;
import com.yourapp.catalog.download.service.DownloadService;
import com.yourapp.catalog.version.entity.VersionEntity;
import com.yourapp.catalog.version.repository.VersionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadServiceTest {

    @Mock
    private VersionRepository versionRepository;

    @Mock
    private DownloadEventRepository downloadEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private DownloadService service;

    private VersionEntity version;

    @BeforeEach
    void setUp() {
        version = new VersionEntity();
        version.setId(UUID.randomUUID().toString());
        version.setVersionNumber("1.0.0");
        version.setFileRef("https://downloads.example.com/app-1.0.0.zip");
        SoftwareEntity software = new SoftwareEntity();
        software.setId(UUID.randomUUID().toString());
        version.setSoftware(software);
    }

    @Test
    void recordAndResolve_shouldRecordEventAndReturnUrl() throws Exception {
        when(versionRepository.findById(version.getId())).thenReturn(Optional.of(version));
        when(downloadEventRepository.save(any(DownloadEventEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        DownloadResponse response = service.recordAndResolve(version.getId(), "user-123");

        assertThat(response.getVersionId()).isEqualTo(version.getId());
        assertThat(response.getDownloadUrl()).isEqualTo(version.getFileRef());
        verify(downloadEventRepository).save(any(DownloadEventEntity.class));
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    void recordAndResolve_shouldThrow_whenVersionNotFound() {
        when(versionRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordAndResolve("missing-id", "user-123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-id");
    }
}
