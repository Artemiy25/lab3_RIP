-- Initial database schema for Andesis application
CREATE TABLE IF NOT EXISTS statistics_logs (
    id SERIAL PRIMARY KEY,
    request_count BIGINT NOT NULL,
    min_value BIGINT NOT NULL,
    max_value BIGINT NOT NULL,
    mean DOUBLE PRECISION NOT NULL,
    standard_deviation DOUBLE PRECISION NOT NULL,
    processing_time_ms BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_statistics_created_at ON statistics_logs(created_at);
