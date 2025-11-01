CREATE TABLE IF NOT EXISTS genre (
    id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
      is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL
);

CREATE INDEX idx_genre_name ON genre(name);
CREATE INDEX idx_genre_deleted_at ON genre(deleted_at);

INSERT IGNORE INTO genre (id, name, description) VALUES
(UUID_TO_BIN(UUID()), 'Action', 'Movies with physical action, stunts, and fights'),
(UUID_TO_BIN(UUID()), 'Comedy', 'Funny movies intended to make audiences laugh'),
(UUID_TO_BIN(UUID()), 'Drama', 'Serious stories focusing on emotional development'),
(UUID_TO_BIN(UUID()), 'Horror', 'Movies designed to scare and shock viewers'),
(UUID_TO_BIN(UUID()), 'Sci-Fi', 'Futuristic technology, space travel, and aliens');
