package com.yourapp.catalog.version;

import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.software.entity.SoftwareEntity;
import com.yourapp.catalog.software.event.SoftwareIndexEvent;
import com.yourapp.catalog.software.event.SoftwareIndexEvent.Action;
import com.yourapp.catalog.software.repository.SoftwareRepository;
import com.yourapp.catalog.version.dto.request.CreateVersionRequest;
import com.yourapp.catalog.version.dto.response.VersionResponse;
import com.yourapp.catalog.version.entity.VersionEntity;
import com.yourapp.catalog.version.mapper.VersionMapper;
import com.yourapp.catalog.version.repository.VersionRepository;
import com.yourapp.catalog.version.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private VersionRepository versionRepository;

    @Mock
    private SoftwareRepository softwareRepository;

    @Mock
    private VersionMapper mapper;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private VersionService service;

    private SoftwareEntity software;
    private VersionEntity versionEntity;
    private VersionResponse versionResponse;

    @BeforeEach
    void setUp() {
        software = new SoftwareEntity();
        software.setId(UUID.randomUUID().toString());

        versionEntity = new VersionEntity();
        versionEntity.setId(UUID.randomUUID().toString());
        versionEntity.setSoftware(software);
        versionEntity.setVersionNumber("1.0.0");

        versionResponse = new VersionResponse();
        versionResponse.setId(versionEntity.getId());
        versionResponse.setVersionNumber("1.0.0");
        versionResponse.setSoftwareId(software.getId());
    }

    @Test
    void create_shouldSaveAndReturnResponse() {
        CreateVersionRequest request = new CreateVersionRequest();
        request.setVersionNumber("1.0.0");

        when(softwareRepository.findById(software.getId())).thenReturn(Optional.of(software));
        when(mapper.toEntity(request)).thenReturn(versionEntity);
        when(versionRepository.save(versionEntity)).thenReturn(versionEntity);
        when(mapper.toResponse(versionEntity)).thenReturn(versionResponse);

        VersionResponse result = service.create(software.getId(), request);

        assertThat(result.getVersionNumber()).isEqualTo("1.0.0");
        verify(events).publishEvent(new SoftwareIndexEvent(software.getId(), Action.UPSERT));
    }

    @Test
    void create_shouldThrow_whenSoftwareNotFound() {
        when(softwareRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("bad-id", new CreateVersionRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBySoftwareId_shouldReturnList() {
        when(versionRepository.findBySoftwareId(eq(software.getId()), any(Sort.class)))
                .thenReturn(List.of(versionEntity));
        when(mapper.toResponse(versionEntity)).thenReturn(versionResponse);

        List<VersionResponse> result = service.findBySoftwareId(software.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void delete_shouldPublishUpsertForTheParentSoftware() {
        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));

        service.delete(versionEntity.getId());

        verify(versionRepository).delete(versionEntity);
        verify(events).publishEvent(new SoftwareIndexEvent(software.getId(), Action.UPSERT));
    }
}

