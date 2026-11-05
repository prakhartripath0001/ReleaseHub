package com.search_service.web;

import com.search_service.search.SearchQueryService;
import com.search_service.search.SearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchQueryService searchQueryService;

    public SearchController(SearchQueryService searchQueryService) {
        this.searchQueryService = searchQueryService;
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String os,
                               @RequestParam(required = false) String category,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return searchQueryService.search(q, os, category, page, size);
    }
}
