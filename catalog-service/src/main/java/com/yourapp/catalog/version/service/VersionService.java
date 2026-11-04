package com.yourapp.catalog.version.service;

import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.software.entity.SoftwareEntity;
import com.yourapp.catalog.software.repository.SoftwareRepository;
import com.yourapp.catalog.version.dto.request.CreateVersionRequest;
import com.yourapp.catalog.version.dto.response.VersionResponse;
import com.yourapp.catalog.version.entity.VersionEntity;
import com.yourapp.catalog.version.mapper.VersionMapper;
import com.yourapp.catalog.version.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;
    private final SoftwareRepository softwareRepository;
    private final VersionMapper mapper;

    @Transactional
    public VersionResponse create(String softwareId, CreateVersionRequest request) {
        SoftwareEntity software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found: " + softwareId));
        VersionEntity entity = mapper.toEntity(request);
        entity.setSoftware(software);
        return mapper.toResponse(versionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> findBySoftwareId(String softwareId) {
        return versionRepository
                .findBySoftwareId(softwareId, Sort.by("releasedAt").descending())
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VersionResponse findById(String id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public void delete(String id) {
        getOrThrow(id);
        versionRepository.deleteById(id);
    }

    private VersionEntity getOrThrow(String id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + id));
    }
}
