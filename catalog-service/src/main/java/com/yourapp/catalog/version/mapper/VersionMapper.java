package com.yourapp.catalog.version.mapper;

import com.yourapp.catalog.version.dto.request.CreateVersionRequest;
import com.yourapp.catalog.version.dto.response.VersionResponse;
import com.yourapp.catalog.version.entity.VersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VersionMapper {

    @Mapping(target = "software", ignore = true)
    VersionEntity toEntity(CreateVersionRequest request);

    @Mapping(source = "software.id", target = "softwareId")
    VersionResponse toResponse(VersionEntity entity);
}
