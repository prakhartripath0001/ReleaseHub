package com.yourapp.catalog.software.repository;

import com.yourapp.catalog.software.entity.SoftwareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SoftwareRepository extends JpaRepository<SoftwareEntity, String>,
        JpaSpecificationExecutor<SoftwareEntity> {
}
