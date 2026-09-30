ALTER TABLE urls ADD COLUMN normalized_original_url TEXT;

WITH ranked_urls AS (
    SELECT id,
           btrim(original_url) AS normalized_url,
           row_number() OVER (
               PARTITION BY btrim(original_url)
               ORDER BY (expires_at IS NULL OR expires_at > NOW()) DESC, created_at DESC, id DESC
           ) AS row_number
    FROM urls
)
UPDATE urls AS target
SET normalized_original_url = ranked_urls.normalized_url
FROM ranked_urls
WHERE target.id = ranked_urls.id
  AND ranked_urls.row_number = 1;

CREATE UNIQUE INDEX uq_urls_normalized_original_url
    ON urls (normalized_original_url)
    WHERE normalized_original_url IS NOT NULL;

ALTER TABLE urls RENAME CONSTRAINT urls_short_code_key TO uq_urls_short_code;

CREATE TABLE released_short_codes (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(32) NOT NULL,
    released_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_released_short_codes_short_code UNIQUE (short_code)
);

CREATE INDEX idx_released_short_codes_released_at
    ON released_short_codes (released_at, id);