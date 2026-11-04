package com.yourapp.catalog.download.controller;

import com.yourapp.catalog.download.dto.response.DownloadResponse;
import com.yourapp.catalog.download.service.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/download")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService service;

    @GetMapping("/{versionId}")
    public ResponseEntity<DownloadResponse> download(
            @PathVariable String versionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.recordAndResolve(versionId, userId));
    }
}

