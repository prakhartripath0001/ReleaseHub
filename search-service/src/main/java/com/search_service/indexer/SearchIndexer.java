package com.search_service.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.search_service.catalog.SoftwareCatalogReader;
import com.search_service.index.SoftwareDocument;
import com.search_service.index.SoftwareIndexManager;
import com.search_service.support.ElasticsearchCalls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Service
public class SearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);
    private static final int BULK_BATCH = 500;

    private final ElasticsearchClient client;
    private final SoftwareIndexManager indexManager;
    private final SoftwareCatalogReader catalogReader;

    public SearchIndexer(ElasticsearchClient client,
                         SoftwareIndexManager indexManager,
                         SoftwareCatalogReader catalogReader) {
        this.client = client;
        this.indexManager = indexManager;
        this.catalogReader = catalogReader;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSoftwareIndexEvent(SoftwareIndexEvent event) {
        sync(event.softwareId(), event.action());
    }

    public void sync(String softwareId, IndexAction action) {
        if (action == IndexAction.DELETE) {
            delete(softwareId);
            return;
        }
        catalogReader.load(softwareId).ifPresentOrElse(this::index, () -> {
            log.info("Software {} is gone; removing it from the search index", softwareId);
            delete(softwareId);
        });
    }

    public void index(SoftwareDocument document) {
        ElasticsearchCalls.call(() -> client.index(i -> i
                .index(indexManager.alias())
                .id(document.getId())
                .document(document)
                .refresh(Refresh.WaitFor)));
    }

    public void delete(String softwareId) {
        ElasticsearchCalls.call(() -> client.delete(d -> d
                .index(indexManager.alias())
                .id(softwareId)
                .refresh(Refresh.WaitFor)));
    }

    public void bulkIndex(String targetIndex, List<SoftwareDocument> documents) {
        if (documents.isEmpty()) {
            return;
        }
        for (int offset = 0; offset < documents.size(); offset += BULK_BATCH) {
            List<SoftwareDocument> batch = documents.subList(offset, Math.min(offset + BULK_BATCH, documents.size()));
            BulkRequest.Builder builder = new BulkRequest.Builder().refresh(Refresh.WaitFor);
            for (SoftwareDocument document : batch) {
                builder.operations(op -> op.index(idx -> idx
                        .index(targetIndex)
                        .id(document.getId())
                        .document(document)));
            }
            BulkResponse response = ElasticsearchCalls.call(() -> client.bulk(builder.build()));
            if (response.errors()) {
                String reason = response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.id() + ": " + item.error().reason())
                        .findFirst()
                        .orElse("unknown bulk error");
                throw new IllegalStateException("Bulk index into " + targetIndex + " failed: " + reason);
            }
        }
    }
}
