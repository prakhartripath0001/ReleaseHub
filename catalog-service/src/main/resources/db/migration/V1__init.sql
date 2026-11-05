CREATE TABLE IF NOT EXISTS software (
    id           VARCHAR(36)  NOT NULL DEFAULT (UUID()),
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    category     VARCHAR(100),
    tags         JSON,
    publisher_id VARCHAR(36)  NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS version (
    id             VARCHAR(36)  NOT NULL DEFAULT (UUID()),
    software_id    VARCHAR(36)  NOT NULL,
    version_number VARCHAR(50)  NOT NULL,
    os             VARCHAR(50),
    release_notes  TEXT,
    file_ref       VARCHAR(500),
    released_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_version_software
        FOREIGN KEY (software_id)
        REFERENCES software (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS download_event (
    id         VARCHAR(36) NOT NULL DEFAULT (UUID()),
    version_id VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36),
    ts         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_download_version
        FOREIGN KEY (version_id)
        REFERENCES version (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_software_publisher_id ON software (publisher_id);
CREATE INDEX idx_software_category ON software (category);
CREATE INDEX idx_software_name ON software (name);
CREATE INDEX idx_version_software_released ON version (software_id, released_at DESC);
CREATE INDEX idx_download_event_version_id ON download_event (version_id);
CREATE INDEX idx_download_event_ts ON download_event (ts DESC);
