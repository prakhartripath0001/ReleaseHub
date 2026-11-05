package com.search_service.indexer;

import com.search_service.catalog.SoftwareCatalogReader;
import com.search_service.index.SoftwareDocument;
import com.search_service.index.SoftwareIndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Rebuilds a new {@code software_vN} index from MySQL and atomically swaps the {@code software} alias.
 * Searches keep hitting the previous index until the swap, so the endpoint stays available.
 */
@Service
public class ReindexJob {

    private static final Logger log = LoggerFactory.getLogger(ReindexJob.class);

    private final SoftwareCatalogReader catalogReader;
    private final SoftwareIndexManager indexManager;
    private final SearchIndexer searchIndexer;

    public ReindexJob(SoftwareCatalogReader catalogReader,
                      SoftwareIndexManager indexManager,
                      SearchIndexer searchIndexer) {
        this.catalogReader = catalogReader;
        this.indexManager = indexManager;
        this.searchIndexer = searchIndexer;
    }

    public ReindexResult reindex() {
        return reindex(ReindexHooks.NOOP);
    }

    public ReindexResult reindex(ReindexHooks hooks) {
        String current = indexManager.currentIndex();
        String next = indexManager.nextIndexName();
        log.info("Reindex started. Live index is {}, staging index is {}", current, next);
        boolean swapped = false;
        try {
            indexManager.createIndex(next);
            List<SoftwareDocument> documents = catalogReader.loadAll();
            searchIndexer.bulkIndex(next, documents);
            hooks.beforeAliasSwap(current, next);
            indexManager.swapAlias(current, next);
            swapped = true;
            deleteQuietly(current);
            log.info("Reindex finished. Alias {} now points at {} ({} documents)",
                    indexManager.alias(), next, documents.size());
            return new ReindexResult(indexManager.alias(), current, next, documents.size());
        } catch (RuntimeException ex) {
            if (!swapped) {
                log.warn("Reindex into {} failed. Leaving alias {} on {}", next, indexManager.alias(), current);
                deleteQuietly(next);
            }
            throw ex;
        }
    }

    private void deleteQuietly(String index) {
        try {
            indexManager.deleteIndex(index);
        } catch (RuntimeException ex) {
            log.warn("Could not delete index {}", index, ex);
        }
    }
}
