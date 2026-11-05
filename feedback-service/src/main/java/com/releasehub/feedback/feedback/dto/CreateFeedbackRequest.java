package com.releasehub.feedback.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotBlank @Size(max = 36) String versionId,
        @NotBlank @Size(max = 36) String userId,
        @Min(1) @Max(5) int rating,
        @Size(max = 4000) String comment
) {
}
