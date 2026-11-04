package com.yourapp.catalog.software.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateSoftwareRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String category;
    private List<String> tags;

    private String publisherId;

    public String getPublisherId() {
        if (publisherId == null || publisherId.isBlank()) {
            return "pub-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return publisherId;
    }
}
