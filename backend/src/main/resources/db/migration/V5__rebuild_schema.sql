DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS articles CASCADE;
DROP TABLE IF EXISTS news_fetch_state CASCADE;
DROP TABLE IF EXISTS integration_settings CASCADE;
DROP TABLE IF EXISTS beat_query_recipes CASCADE;
DROP TABLE IF EXISTS beats CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE beats (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE beat_query_recipes (
    id BIGSERIAL PRIMARY KEY,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
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
    id BIGSERIAL PRIMARY KEY,
    provider_type TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    api_key_encrypted TEXT,
    default_lang TEXT,
    default_country TEXT,
    refresh_interval_minutes INTEGER NOT NULL DEFAULT 30,
    ttl_minutes INTEGER NOT NULL DEFAULT 15,
    max_per_request INTEGER NOT NULL DEFAULT 50,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES users(id)
);

CREATE TABLE news_fetch_state (
    id BIGSERIAL PRIMARY KEY,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    last_success_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    last_error_code TEXT,
    last_error_message TEXT,
    consecutive_failures INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    provider_type TEXT NOT NULL,
    provider_article_id TEXT,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
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
    published_by BIGINT REFERENCES users(id),
    internal_published_at_utc TIMESTAMPTZ
);

CREATE UNIQUE INDEX articles_url_idx ON articles (url);
CREATE UNIQUE INDEX articles_provider_idx ON articles (provider_type, provider_article_id);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES users(id),
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata_jsonb JSONB
);

INSERT INTO users (email, password_hash, role)
VALUES
  ('admin@example.com', '{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOQ2lyxY9s/6ag7ss9nKi7Lr/vEwV0y', 'ADMIN'),
  ('member@example.com', '{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOQ2lyxY9s/6ag7ss9nKi7Lr/vEwV0y', 'MEMBER');

INSERT INTO beats (name, slug, is_active)
VALUES
  ('Law', 'law', TRUE),
  ('Taxation', 'taxation', TRUE),
  ('Healthcare', 'healthcare', TRUE),
  ('Education', 'education', TRUE),
  ('Steel', 'steel', TRUE),
  ('Finance/BFSI', 'finance-bfsi', TRUE),
  ('Tech/AI', 'tech-ai', TRUE),
  ('Telecom', 'telecom', TRUE),
  ('Energy', 'energy', TRUE),
  ('ESG', 'esg', TRUE),
  ('Manufacturing', 'manufacturing', TRUE),
  ('Startups/VC', 'startups-vc', TRUE),
  ('Real Estate & Infra', 'real-estate-infra', TRUE),
  ('Pharma/MedTech', 'pharma-medtech', TRUE),
  ('Consumer/Retail', 'consumer-retail', TRUE),
  ('Auto/EV', 'auto-ev', TRUE);

INSERT INTO beat_query_recipes (beat_id, endpoint_type, q, lang, country, max, sort)
SELECT id,
       'SEARCH',
       name,
       'en',
       'us',
       25,
       'publishedAt'
FROM beats;

INSERT INTO integration_settings (provider_type, is_enabled, api_key_encrypted, default_lang, default_country, refresh_interval_minutes, ttl_minutes, max_per_request, updated_by)
VALUES ('GNEWS', TRUE, NULL, 'en', 'us', 30, 15, 50, 1);

INSERT INTO news_fetch_state (beat_id, consecutive_failures)
SELECT id, 0 FROM beats;

INSERT INTO articles (provider_type, provider_article_id, beat_id, title, description, content, url, image_url, published_at_utc, lang, source_id, source_name, source_url, source_country, raw_payload_jsonb, status)
SELECT
  'MOCK',
  'seed-' || gs,
  ((gs - 1) % 16) + 1,
  'Seeded article ' || gs,
  'Seeded description for article ' || gs,
  'Seeded content for article ' || gs,
  'https://news.example.com/article-' || gs,
  'https://images.example.com/article-' || gs || '.jpg',
  NOW() - ((gs % 10) || ' hours')::interval,
  'en',
  'source-' || ((gs % 5) + 1),
  'Source ' || ((gs % 5) + 1),
  'https://source.example.com/' || ((gs % 5) + 1),
  'US',
  jsonb_build_object('seed', true),
  'INGESTED'
FROM generate_series(1, 50) AS gs;
