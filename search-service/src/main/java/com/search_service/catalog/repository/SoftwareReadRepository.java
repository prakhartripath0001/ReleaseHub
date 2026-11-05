package com.search_service.catalog.repository;

import com.search_service.catalog.entity.SoftwareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoftwareReadRepository extends JpaRepository<SoftwareEntity, String> {
}
