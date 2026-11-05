package com.analytics_service.report;

import java.math.BigDecimal;

public record ReportResponse(
        String softwareId,
        long downloads,
        BigDecimal avgRating,
        long feedbackCount
) {
}
