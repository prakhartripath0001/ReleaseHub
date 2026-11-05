package com.search_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "releasehub.search")
public record SearchIndexProperties(String alias, String indexPrefix) {
}
