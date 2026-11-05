package com.search_service.indexer;

@FunctionalInterface
public interface ReindexHooks {

    ReindexHooks NOOP = (currentIndex, stagingIndex) -> {
    };

    void beforeAliasSwap(String currentIndex, String stagingIndex);
}
