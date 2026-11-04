package com.yourapp.catalog.download.repository;

import com.yourapp.catalog.download.entity.DownloadEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DownloadEventRepository extends JpaRepository<DownloadEventEntity, String> {
}

