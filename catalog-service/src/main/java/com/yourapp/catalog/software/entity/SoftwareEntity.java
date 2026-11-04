package com.yourapp.catalog.software.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "software")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> tags;

    @Column(name = "publisher_id", length = 36, nullable = false)
    private String publisherId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
