package com.yourapp.catalog.version.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateVersionRequest {

    @NotBlank(message = "Version number is required")
    private String versionNumber;

    private String os;
    private String releaseNotes;
    private String fileRef;
}

