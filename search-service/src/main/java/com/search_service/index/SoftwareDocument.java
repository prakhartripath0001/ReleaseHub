package com.search_service.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareDocument {

    private String id;
    private String name;
    private String description;
    private String category;
    private List<String> tags;
    private List<String> os;
}
