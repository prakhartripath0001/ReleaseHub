package com.yourapp.catalog.download.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "download_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadEventEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "version_id", length = 36, nullable = false)
    private String versionId;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "ts")
    private LocalDateTime ts;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (ts == null) ts = LocalDateTime.now();
    }
}
