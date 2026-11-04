package com.yourapp.catalog.software.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateSoftwareRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String category;
    private List<String> tags;

    @NotBlank(message = "Publisher ID is required")
    private String publisherId;
}
