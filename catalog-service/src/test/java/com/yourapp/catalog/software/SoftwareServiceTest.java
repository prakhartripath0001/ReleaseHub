package com.yourapp.catalog.software;

import com.yourapp.catalog.common.dto.PageResponse;
import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.software.dto.request.CreateSoftwareRequest;
import com.yourapp.catalog.software.dto.request.UpdateSoftwareRequest;
import com.yourapp.catalog.software.dto.response.SoftwareResponse;
import com.yourapp.catalog.software.entity.SoftwareEntity;
import com.yourapp.catalog.software.mapper.SoftwareMapper;
import com.yourapp.catalog.software.repository.SoftwareRepository;
import com.yourapp.catalog.software.service.SoftwareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoftwareServiceTest {

    @Mock
    private SoftwareRepository repository;

    @Mock
    private SoftwareMapper mapper;

    @InjectMocks
    private SoftwareService service;

    private SoftwareEntity entity;
    private SoftwareResponse response;

    @BeforeEach
    void setUp() {
        entity = new SoftwareEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName("TestApp");
        entity.setCategory("tools");

        response = new SoftwareResponse();
        response.setId(entity.getId());
        response.setName("TestApp");
    }

    @Test
    void create_shouldReturnResponse() {
        CreateSoftwareRequest request = new CreateSoftwareRequest();
        request.setName("TestApp");
        request.setPublisherId(UUID.randomUUID().toString());

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        SoftwareResponse result = service.create(request);

        assertThat(result.getName()).isEqualTo("TestApp");
        verify(repository).save(entity);
    }

    @Test
    void findById_shouldReturnResponse_whenExists() {
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        SoftwareResponse result = service.findById(entity.getId());

        assertThat(result.getId()).isEqualTo(entity.getId());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-id");
    }

    @Test
    void delete_shouldCallRepository() {
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        service.delete(entity.getId());

        verify(repository).deleteById(entity.getId());
    }

    @Test
    void findAll_shouldReturnPageResponse() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toResponse(entity)).thenReturn(response);

        PageResponse<SoftwareResponse> result = service.findAll(null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}

