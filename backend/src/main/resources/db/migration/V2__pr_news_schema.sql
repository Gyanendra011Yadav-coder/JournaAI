DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS articles CASCADE;
DROP TABLE IF EXISTS news_fetch_state CASCADE;
DROP TABLE IF EXISTS integration_settings CASCADE;
DROP TABLE IF EXISTS beat_query_recipes CASCADE;
DROP TABLE IF EXISTS beats CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE beats (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE beat_query_recipes (
    id SERIAL PRIMARY KEY,
    beat_id INTEGER NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    endpoint_type TEXT NOT NULL CHECK (endpoint_type IN ('SEARCH', 'TOP_HEADLINES')),
    q TEXT,
    category TEXT,
    lang TEXT,
    country TEXT,
    in_fields TEXT,
    nullable_fields TEXT,
    max INTEGER,
    sort TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE integration_settings (
    id SERIAL PRIMARY KEY,
    provider_type TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    api_key_encrypted TEXT,
    default_lang TEXT,
    default_country TEXT,
    refresh_interval_minutes INTEGER NOT NULL DEFAULT 30,
    ttl_minutes INTEGER NOT NULL DEFAULT 15,
    max_per_request INTEGER NOT NULL DEFAULT 50,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by INTEGER REFERENCES users(id)
);

CREATE TABLE news_fetch_state (
    id SERIAL PRIMARY KEY,
    beat_id INTEGER NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    last_success_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    last_error_code TEXT,
    last_error_message TEXT,
    consecutive_failures INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    provider_type TEXT NOT NULL,
    provider_article_id TEXT,
    beat_id INTEGER NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    content TEXT,
    url TEXT NOT NULL,
    image_url TEXT,
    published_at_utc TIMESTAMPTZ,
    lang TEXT,
    source_id TEXT,
    source_name TEXT,
    source_url TEXT,
    source_country TEXT,
    raw_payload_jsonb JSONB,
    fetched_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status TEXT NOT NULL CHECK (status IN ('INGESTED', 'PUBLISHED')),
    published_by INTEGER REFERENCES users(id),
    internal_published_at_utc TIMESTAMPTZ
);

CREATE UNIQUE INDEX articles_url_idx ON articles (url);
CREATE UNIQUE INDEX articles_provider_idx ON articles (provider_type, provider_article_id);

CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    actor_user_id INTEGER REFERENCES users(id),
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata_jsonb JSONB
);
