package com.search_service.catalog.repository;

import com.search_service.catalog.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionReadRepository extends JpaRepository<VersionEntity, String> {

    List<VersionEntity> findBySoftwareId(String softwareId);
}
