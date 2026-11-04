package com.yourapp.catalog.download.service;

import com.yourapp.catalog.common.exception.ResourceNotFoundException;
import com.yourapp.catalog.download.dto.response.DownloadResponse;
import com.yourapp.catalog.download.entity.DownloadEventEntity;
import com.yourapp.catalog.download.repository.DownloadEventRepository;
import com.yourapp.catalog.version.entity.VersionEntity;
import com.yourapp.catalog.version.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final VersionRepository versionRepository;
    private final DownloadEventRepository downloadEventRepository;

    @Transactional
    public DownloadResponse recordAndResolve(String versionId, String userId) {
        VersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

        downloadEventRepository.save(
                DownloadEventEntity.builder()
                        .versionId(versionId)
                        .userId(userId)
                        .build()
        );

        DownloadResponse response = new DownloadResponse();
        response.setVersionId(versionId);
        response.setDownloadUrl(
                version.getFileRef() != null ? version.getFileRef() : "/files/" + versionId
        );
        response.setMessage("Download recorded successfully");
        return response;
    }
}

