package com.yourapp.catalog.software.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateSoftwareRequest {
    private String name;
    private String description;
    private String category;
    private List<String> tags;
}
