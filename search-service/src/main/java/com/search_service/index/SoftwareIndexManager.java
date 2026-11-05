package com.search_service.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import com.search_service.config.SearchIndexProperties;
import com.search_service.support.ElasticsearchCalls;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Owns the versioned {@code software_vN} indices and the read/write alias {@code software}.
 * Mapping-level {@code boost} was removed in Elasticsearch 5+ and is rejected by 8.x.
 * Name ranking is applied at query time as {@code name^3}.
 */
@Component
public class SoftwareIndexManager {

    private final ElasticsearchClient client;
    private final SearchIndexProperties properties;

    public SoftwareIndexManager(ElasticsearchClient client, SearchIndexProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public String alias() {
        return properties.alias();
    }

    public void ensureAlias() {
        ElasticsearchCalls.call(() -> {
            boolean aliasExists = client.indices().existsAlias(e -> e.name(properties.alias())).value();
            if (aliasExists) {
                return null;
            }
            String index = properties.indexPrefix() + "1";
            if (!client.indices().exists(e -> e.index(index)).value()) {
                createIndex(index);
            }
            client.indices().updateAliases(a -> a.actions(action ->
                    action.add(add -> add.index(index).alias(properties.alias()))));
            return null;
        });
    }

    public void createIndex(String name) {
        ElasticsearchCalls.call(() -> {
            ClassPathResource resource = new ClassPathResource("elasticsearch/software-index.json");
            try (InputStream in = resource.getInputStream()) {
                client.indices().create(c -> c.index(name).withJson(in));
            }
            return null;
        });
    }

    public String currentIndex() {
        return ElasticsearchCalls.call(() -> {
            GetAliasResponse response = client.indices().getAlias(g -> g.name(properties.alias()));
            return response.result().keySet().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Alias " + properties.alias() + " has no index"));
        });
    }

    public String nextIndexName() {
        return ElasticsearchCalls.call(() -> {
            int max = 0;
            GetIndexResponse response = client.indices().get(g -> g
                    .index(properties.indexPrefix() + "*")
                    .ignoreUnavailable(true)
                    .allowNoIndices(true));
            for (String name : response.result().keySet()) {
                max = Math.max(max, versionOf(name));
            }
            return properties.indexPrefix() + (max + 1);
        });
    }

    public void swapAlias(String fromIndex, String toIndex) {
        ElasticsearchCalls.call(() -> {
            client.indices().updateAliases(u -> u
                    .actions(a -> a.remove(r -> r.index(fromIndex).alias(properties.alias())))
                    .actions(a -> a.add(ad -> ad.index(toIndex).alias(properties.alias()))));
            return null;
        });
    }

    public void deleteIndex(String name) {
        ElasticsearchCalls.call(() -> {
            if (client.indices().exists(e -> e.index(name)).value()) {
                client.indices().delete(d -> d.index(name));
            }
            return null;
        });
    }

    public boolean indexExists(String name) {
        return ElasticsearchCalls.call(() -> client.indices().exists(e -> e.index(name)).value());
    }

    private int versionOf(String indexName) {
        String prefix = properties.indexPrefix();
        if (!indexName.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(indexName.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
