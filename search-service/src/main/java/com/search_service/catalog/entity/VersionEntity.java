package com.search_service.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "version")
@Getter
@Setter
public class VersionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "software_id", length = 36, nullable = false)
    private String softwareId;

    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    private String os;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "file_ref")
    private String fileRef;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
