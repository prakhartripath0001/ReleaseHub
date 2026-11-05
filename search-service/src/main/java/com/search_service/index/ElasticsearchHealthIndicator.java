package com.search_service.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.search_service.support.ElasticsearchCalls;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private final ElasticsearchClient client;

    public ElasticsearchHealthIndicator(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            boolean reachable = ElasticsearchCalls.call(() -> client.ping().value());
            return reachable ? Health.up().build() : Health.down().withDetail("ping", false).build();
        } catch (RuntimeException ex) {
            return Health.down(ex).build();
        }
    }
}
