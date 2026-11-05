CREATE TABLE feedback (
    id          VARCHAR(36)  NOT NULL,
    version_id  VARCHAR(36)  NOT NULL,
    software_id VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    rating      SMALLINT     NOT NULL,
    comment     TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_feedback_user_version UNIQUE (user_id, version_id),
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_feedback_version
        FOREIGN KEY (version_id) REFERENCES version (id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_software
        FOREIGN KEY (software_id) REFERENCES software (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_feedback_software_created ON feedback (software_id, created_at DESC);

CREATE TABLE outbox (
    id           VARCHAR(36)  NOT NULL,
    aggregate_id VARCHAR(36)  NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSON         NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_outbox_pending ON outbox (published_at, created_at);
