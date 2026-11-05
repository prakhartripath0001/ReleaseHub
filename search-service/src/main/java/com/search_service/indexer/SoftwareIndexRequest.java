package com.search_service.indexer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SoftwareIndexRequest(
        @NotBlank String softwareId,
        @NotNull IndexAction action
) {
}
