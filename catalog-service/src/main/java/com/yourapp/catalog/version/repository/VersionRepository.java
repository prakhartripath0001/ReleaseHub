package com.yourapp.catalog.version.repository;

import com.yourapp.catalog.version.entity.VersionEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, String> {
    List<VersionEntity> findBySoftwareId(String softwareId, Sort sort);
}

