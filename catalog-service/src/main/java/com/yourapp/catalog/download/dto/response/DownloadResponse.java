package com.yourapp.catalog.download.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadResponse {
    private String downloadUrl;
    private String versionId;
    private String message;
}

