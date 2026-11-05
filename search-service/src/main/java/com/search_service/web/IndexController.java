package com.search_service.web;

import com.search_service.indexer.ReindexJob;
import com.search_service.indexer.ReindexResult;
import com.search_service.indexer.SearchIndexer;
import com.search_service.indexer.SoftwareIndexRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    private final SearchIndexer searchIndexer;
    private final ReindexJob reindexJob;

    public IndexController(SearchIndexer searchIndexer, ReindexJob reindexJob) {
        this.searchIndexer = searchIndexer;
        this.reindexJob = reindexJob;
    }

    @PostMapping("/internal/index")
    public ResponseEntity<Void> sync(@Valid @RequestBody SoftwareIndexRequest request) {
        searchIndexer.sync(request.softwareId(), request.action());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/reindex")
    public ReindexResult reindex() {
        return reindexJob.reindex();
    }
}
