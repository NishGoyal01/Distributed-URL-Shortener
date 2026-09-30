CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(32) UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    click_count BIGINT NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMPTZ
);

CREATE INDEX idx_urls_short_code ON urls (short_code);
CREATE INDEX idx_urls_expires_at ON urls (expires_at);
CREATE INDEX idx_urls_created_at ON urls (created_at);
