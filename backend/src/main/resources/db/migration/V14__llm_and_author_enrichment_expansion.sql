CREATE TABLE IF NOT EXISTS llm_providers (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    base_url TEXT NOT NULL,
    auth_type TEXT NOT NULL CHECK (auth_type IN ('BEARER', 'API_KEY_HEADER', 'QUERY_PARAM')),
    auth_header_name TEXT,
    auth_query_param_name TEXT,
    api_key_encrypted TEXT,
    model TEXT,
    request_template_jsonb JSONB NOT NULL,
    response_jsonpath TEXT NOT NULL,
    timeout_ms INTEGER NOT NULL DEFAULT 10000,
    retry_policy_jsonb JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS article_attributions (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    bureau_name TEXT NOT NULL,
    attribution_type TEXT NOT NULL CHECK (attribution_type IN ('BUREAU', 'NON_PERSON')),
    confidence INTEGER,
    evidence_jsonb JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (article_id)
);

CREATE TABLE IF NOT EXISTS admin_approval_queue (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    change_action TEXT NOT NULL CHECK (change_action IN ('CREATE', 'UPDATE', 'DELETE')),
    proposed_changes_jsonb JSONB NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE article_author_extractions
    ADD COLUMN IF NOT EXISTS method TEXT,
    ADD COLUMN IF NOT EXISTS authors_jsonb JSONB,
    ADD COLUMN IF NOT EXISTS classification TEXT,
    ADD COLUMN IF NOT EXISTS confidence INTEGER,
    ADD COLUMN IF NOT EXISTS evidence_jsonb JSONB,
    ADD COLUMN IF NOT EXISTS status TEXT,
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS content_hash TEXT;

ALTER TABLE journalists
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'AI_FILLED', 'PENDING_ADMIN', 'COMPLETE', 'VERIFIED')),
    ADD COLUMN IF NOT EXISTS author_page_url TEXT,
    ADD COLUMN IF NOT EXISTS twitter TEXT,
    ADD COLUMN IF NOT EXISTS bio_summary TEXT,
    ADD COLUMN IF NOT EXISTS last_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
