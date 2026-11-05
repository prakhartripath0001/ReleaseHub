package com.yourapp.catalog.software.search;

import com.yourapp.catalog.software.event.SoftwareIndexEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Component
public class SearchIndexNotifier {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexNotifier.class);

    private final RestClient searchServiceClient;

    public SearchIndexNotifier(RestClient searchServiceClient) {
        this.searchServiceClient = searchServiceClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSoftwareIndex(SoftwareIndexEvent event) {
        try {
            searchServiceClient.post()
                    .uri("/internal/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Search index notification failed for {}: {}", event.softwareId(), ex.getMessage());
        }
    }
}
