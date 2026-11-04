package com.yourapp.catalog.version.controller;

import com.yourapp.catalog.version.dto.request.CreateVersionRequest;
import com.yourapp.catalog.version.dto.response.VersionResponse;
import com.yourapp.catalog.version.service.VersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VersionController {

    private final VersionService service;

    @PostMapping("/software/{softwareId}/versions")
    public ResponseEntity<VersionResponse> create(@PathVariable String softwareId,
                                                   @Valid @RequestBody CreateVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(softwareId, request));
    }

    @GetMapping("/software/{softwareId}/versions")
    public ResponseEntity<List<VersionResponse>> findBySoftware(@PathVariable String softwareId) {
        return ResponseEntity.ok(service.findBySoftwareId(softwareId));
    }

    @GetMapping("/versions/{id}")
    public ResponseEntity<VersionResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/versions/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

