package com.yourapp.catalog.software.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SoftwareResponse {
    private String id;
    private String name;
    private String description;
    private String category;
    private List<String> tags;
    private String publisherId;
    private LocalDateTime createdAt;
}
