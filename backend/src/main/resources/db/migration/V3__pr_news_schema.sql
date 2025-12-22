DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS outreach_emails CASCADE;
DROP TABLE IF EXISTS outreach_templates CASCADE;
DROP TABLE IF EXISTS journalist_tags CASCADE;
DROP TABLE IF EXISTS journalists CASCADE;
DROP TABLE IF EXISTS news_fetch_state CASCADE;
DROP TABLE IF EXISTS article_tags CASCADE;
DROP TABLE IF EXISTS articles CASCADE;
DROP TABLE IF EXISTS beats CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS saved_searches CASCADE;
DROP TABLE IF EXISTS workspaces CASCADE;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE beats (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE
);

CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    headline TEXT NOT NULL,
    source TEXT,
    author TEXT,
    canonical_url TEXT NOT NULL,
    url TEXT,
    published_at TIMESTAMPTZ,
    summary TEXT,
    provider TEXT NOT NULL,
    raw_payload JSONB,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    saved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX articles_canonical_url_idx ON articles (canonical_url);

CREATE TABLE article_tags (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    UNIQUE (article_id, beat_id)
);

CREATE TABLE news_fetch_state (
    id BIGSERIAL PRIMARY KEY,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    timeframe TEXT NOT NULL,
    last_fetched_at TIMESTAMPTZ,
    failure_count INTEGER NOT NULL DEFAULT 0,
    last_failure_at TIMESTAMPTZ,
    circuit_open_until TIMESTAMPTZ,
    UNIQUE (beat_id, timeframe)
);

CREATE TABLE journalists (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    outlet TEXT,
    location TEXT,
    email TEXT,
    phone TEXT,
    source_provider TEXT NOT NULL,
    provider_reference_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE journalist_tags (
    id BIGSERIAL PRIMARY KEY,
    journalist_id BIGINT NOT NULL REFERENCES journalists(id) ON DELETE CASCADE,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    UNIQUE (journalist_id, beat_id)
);

CREATE TABLE outreach_templates (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    subject TEXT NOT NULL,
    body TEXT NOT NULL
);

CREATE TABLE outreach_emails (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT REFERENCES articles(id),
    journalist_id BIGINT REFERENCES journalists(id),
    template_id BIGINT REFERENCES outreach_templates(id),
    final_subject TEXT NOT NULL,
    final_body TEXT NOT NULL,
    status TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    provider_message_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    entity TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);
