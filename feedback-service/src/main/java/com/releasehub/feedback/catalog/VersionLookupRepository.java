package com.releasehub.feedback.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VersionLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public VersionLookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findSoftwareId(String versionId) {
        return jdbcTemplate.queryForObject(
                "SELECT software_id FROM version WHERE id = ?",
                String.class,
                versionId);
    }
}
