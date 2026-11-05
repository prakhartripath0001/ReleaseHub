package com.search_service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.search_service.index.SoftwareDocument;
import com.search_service.index.SoftwareIndexManager;
import com.search_service.support.ElasticsearchCalls;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RESULT_WINDOW = 10_000;

    private final ElasticsearchClient client;
    private final SoftwareIndexManager indexManager;

    public SearchQueryService(ElasticsearchClient client, SoftwareIndexManager indexManager) {
        this.client = client;
        this.indexManager = indexManager;
    }

    public SearchResult search(String q, String os, String category, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if ((long) page * size >= MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException("page * size must be less than " + MAX_RESULT_WINDOW);
        }

        SearchResponse<SoftwareDocument> response = ElasticsearchCalls.call(() -> client.search(request -> request
                        .index(indexManager.alias())
                        .from(page * size)
                        .size(size)
                        .query(query -> query.bool(bool -> buildBool(bool, q, os, category)))
                        .aggregations("os", agg -> agg.terms(terms -> terms.field("os").size(50)))
                        .aggregations("category", agg -> agg.terms(terms -> terms.field("category").size(50))),
                SoftwareDocument.class));

        List<SearchHit> hits = new ArrayList<>();
        for (Hit<SoftwareDocument> hit : response.hits().hits()) {
            SoftwareDocument source = hit.source();
            if (source == null) {
                continue;
            }
            double score = hit.score() == null ? 0d : hit.score();
            hits.add(new SearchHit(
                    source.getId(),
                    score,
                    source.getName(),
                    source.getDescription(),
                    source.getCategory(),
                    source.getTags(),
                    source.getOs()));
        }

        long total = response.hits().total() == null ? hits.size() : response.hits().total().value();
        Map<String, List<FacetBucket>> facets = new LinkedHashMap<>();
        facets.put("os", buckets(response.aggregations().get("os")));
        facets.put("category", buckets(response.aggregations().get("category")));
        return new SearchResult(hits, facets, page, size, total);
    }

    private BoolQuery.Builder buildBool(BoolQuery.Builder bool, String q, String os, String category) {
        if (StringUtils.hasText(q)) {
            bool.must(must -> must.multiMatch(match -> match
                    .query(q)
                    .fields("name^3", "description")
                    .fuzziness("AUTO")));
        } else {
            bool.must(must -> must.matchAll(all -> all));
        }
        if (StringUtils.hasText(os)) {
            bool.filter(filter -> filter.term(term -> term.field("os").value(os)));
        }
        if (StringUtils.hasText(category)) {
            bool.filter(filter -> filter.term(term -> term.field("category").value(category)));
        }
        return bool;
    }

    private List<FacetBucket> buckets(Aggregate aggregate) {
        if (aggregate == null || !aggregate.isSterms()) {
            return List.of();
        }
        List<FacetBucket> buckets = new ArrayList<>();
        for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
            buckets.add(new FacetBucket(bucket.key().stringValue(), bucket.docCount()));
        }
        return buckets;
    }
}
