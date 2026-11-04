package com.yourapp.catalog.version.entity;

import com.yourapp.catalog.software.entity.SoftwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private SoftwareEntity software;

    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    private String os;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "file_ref")
    private String fileRef;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (releasedAt == null) releasedAt = LocalDateTime.now();
    }
}

