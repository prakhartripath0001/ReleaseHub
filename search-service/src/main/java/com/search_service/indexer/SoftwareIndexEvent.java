package com.search_service.indexer;

public record SoftwareIndexEvent(String softwareId, IndexAction action) {
}
