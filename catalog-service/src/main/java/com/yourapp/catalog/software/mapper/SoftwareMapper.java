package com.yourapp.catalog.software.mapper;

import com.yourapp.catalog.software.dto.request.CreateSoftwareRequest;
import com.yourapp.catalog.software.dto.request.UpdateSoftwareRequest;
import com.yourapp.catalog.software.dto.response.SoftwareResponse;
import com.yourapp.catalog.software.entity.SoftwareEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SoftwareMapper {

    SoftwareEntity toEntity(CreateSoftwareRequest request);

    SoftwareResponse toResponse(SoftwareEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget SoftwareEntity entity, UpdateSoftwareRequest request);
}
