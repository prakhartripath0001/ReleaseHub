package com.search_service.search;

import java.util.List;

public record SearchHit(
        String id,
        double score,
        String name,
        String description,
        String category,
        List<String> tags,
        List<String> os
) {
}
