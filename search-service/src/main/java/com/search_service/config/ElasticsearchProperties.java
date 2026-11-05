package com.search_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "releasehub.elasticsearch")
public record ElasticsearchProperties(String uri) {
}
