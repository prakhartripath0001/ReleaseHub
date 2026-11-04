package com.yourapp.catalog.software.service;

import com.yourapp.catalog.common.dto.PageResponse;
import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.software.dto.request.CreateSoftwareRequest;
import com.yourapp.catalog.software.dto.request.UpdateSoftwareRequest;
import com.yourapp.catalog.software.dto.response.SoftwareResponse;
import com.yourapp.catalog.software.entity.SoftwareEntity;
import com.yourapp.catalog.software.mapper.SoftwareMapper;
import com.yourapp.catalog.software.repository.SoftwareRepository;
import com.yourapp.catalog.software.specification.SoftwareSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SoftwareService {

    private final SoftwareRepository repository;
    private final SoftwareMapper mapper;

    @Transactional
    public SoftwareResponse create(CreateSoftwareRequest request) {
        SoftwareEntity entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public SoftwareResponse findById(String id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SoftwareResponse> findAll(String name, String category, String os,
                                                   int page, int size) {
        Specification<SoftwareEntity> spec = Specification
                .where(SoftwareSpecification.nameLike(name))
                .and(SoftwareSpecification.hasCategory(category))
                .and(SoftwareSpecification.hasOs(os));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SoftwareEntity> result = repository.findAll(spec, pageable);

        PageResponse<SoftwareResponse> response = new PageResponse<>();
        response.setContent(result.getContent().stream().map(mapper::toResponse).toList());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setLast(result.isLast());
        return response;
    }

    @Transactional
    public SoftwareResponse update(String id, UpdateSoftwareRequest request) {
        SoftwareEntity entity = getOrThrow(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private SoftwareEntity getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found: " + id));
    }
}

