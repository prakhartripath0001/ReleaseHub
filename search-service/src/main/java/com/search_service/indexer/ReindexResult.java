package com.search_service.indexer;

public record ReindexResult(
        String alias,
        String previousIndex,
        String index,
        int indexed
) {
}
