package com.search_service.search;

import java.util.List;
import java.util.Map;

public record SearchResult(
        List<SearchHit> hits,
        Map<String, List<FacetBucket>> facets,
        int page,
        int size,
        long total
) {
}
