package com.search_service.catalog;

import com.search_service.catalog.entity.SoftwareEntity;
import com.search_service.catalog.entity.VersionEntity;
import com.search_service.catalog.repository.SoftwareReadRepository;
import com.search_service.catalog.repository.VersionReadRepository;
import com.search_service.index.SoftwareDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SoftwareCatalogReader {

    private final SoftwareReadRepository softwareRepository;
    private final VersionReadRepository versionRepository;

    public SoftwareCatalogReader(SoftwareReadRepository softwareRepository,
                                 VersionReadRepository versionRepository) {
        this.softwareRepository = softwareRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SoftwareDocument> load(String softwareId) {
        return softwareRepository.findById(softwareId)
                .map(software -> toDocument(software, versionRepository.findBySoftwareId(softwareId)));
    }

    @Transactional(readOnly = true)
    public List<SoftwareDocument> loadAll() {
        Map<String, List<VersionEntity>> versionsBySoftware = versionRepository.findAll().stream()
                .collect(Collectors.groupingBy(VersionEntity::getSoftwareId));
        List<SoftwareDocument> documents = new ArrayList<>();
        for (SoftwareEntity software : softwareRepository.findAll()) {
            documents.add(toDocument(software, versionsBySoftware.getOrDefault(software.getId(), List.of())));
        }
        return documents;
    }

    private SoftwareDocument toDocument(SoftwareEntity software, List<VersionEntity> versions) {
        LinkedHashSet<String> operatingSystems = new LinkedHashSet<>();
        for (VersionEntity version : versions) {
            if (version.getOs() != null && !version.getOs().isBlank()) {
                operatingSystems.add(version.getOs());
            }
        }
        List<String> tags = software.getTags() == null ? List.of() : List.copyOf(software.getTags());
        return SoftwareDocument.builder()
                .id(software.getId())
                .name(software.getName())
                .description(software.getDescription())
                .category(software.getCategory())
                .tags(tags)
                .os(List.copyOf(operatingSystems))
                .build();
    }
}
