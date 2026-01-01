CREATE TABLE IF NOT EXISTS journalist_enrichment_reviews (
  id BIGSERIAL PRIMARY KEY,
  journalist_id BIGINT NOT NULL REFERENCES journalists(id) ON DELETE CASCADE,
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  current_jsonb JSONB,
  proposed_jsonb JSONB,
  diff_jsonb JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_journalist_enrichment_reviews_status
  ON journalist_enrichment_reviews(status);
