package com.yourapp.catalog.version.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VersionResponse {
    private String id;
    private String softwareId;
    private String versionNumber;
    private String os;
    private String releaseNotes;
    private String fileRef;
    private LocalDateTime releasedAt;
}
