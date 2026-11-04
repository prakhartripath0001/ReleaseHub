package com.yourapp.catalog.software.controller;

import com.yourapp.catalog.common.dto.PageResponse;
import com.yourapp.catalog.software.dto.request.CreateSoftwareRequest;
import com.yourapp.catalog.software.dto.request.UpdateSoftwareRequest;
import com.yourapp.catalog.software.dto.response.SoftwareResponse;
import com.yourapp.catalog.software.service.SoftwareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/software")
@RequiredArgsConstructor
public class SoftwareController {

    private final SoftwareService service;

    @PostMapping
    public ResponseEntity<SoftwareResponse> create(@Valid @RequestBody CreateSoftwareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SoftwareResponse>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String os,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.findAll(name, category, os, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoftwareResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SoftwareResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody UpdateSoftwareRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
