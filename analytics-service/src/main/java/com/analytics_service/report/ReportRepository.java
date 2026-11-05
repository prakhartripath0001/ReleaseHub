package com.analytics_service.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ReportResponse findBySoftwareId(String softwareId) {
        return jdbcTemplate.query("""
                        SELECT software_id, downloads, avg_rating, feedback_count
                        FROM report_aggregate
                        WHERE software_id = ?
                        """,
                resultSet -> resultSet.next()
                        ? new ReportResponse(
                                resultSet.getString("software_id"),
                                resultSet.getLong("downloads"),
                                resultSet.getBigDecimal("avg_rating"),
                                resultSet.getLong("feedback_count"))
                        : new ReportResponse(softwareId, 0, BigDecimal.ZERO, 0),
                softwareId);
    }
}
