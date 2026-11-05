CREATE TABLE processed_event (
    event_id     VARCHAR(36)  NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE report_aggregate (
    software_id   VARCHAR(36) NOT NULL,
    downloads     BIGINT      NOT NULL DEFAULT 0,
    avg_rating    DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    feedback_count BIGINT     NOT NULL DEFAULT 0,
    rating_sum    BIGINT      NOT NULL DEFAULT 0,
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (software_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
