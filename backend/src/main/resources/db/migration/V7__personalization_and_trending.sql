ALTER TABLE beat_query_recipes RENAME TO beat_query_templates;
ALTER TABLE beat_query_templates RENAME COLUMN q TO beat_terms_jsonb;
ALTER TABLE beat_query_templates ALTER COLUMN beat_terms_jsonb TYPE JSONB USING to_jsonb(ARRAY[beat_terms_jsonb]);
ALTER TABLE beat_query_templates RENAME COLUMN lang TO lang_default;
ALTER TABLE beat_query_templates RENAME COLUMN country TO country_default;
ALTER TABLE beat_query_templates RENAME COLUMN in_fields TO in_default;
ALTER TABLE beat_query_templates RENAME COLUMN sort TO sortby_default;
ALTER TABLE beat_query_templates RENAME COLUMN max TO max_default;

ALTER TABLE articles ALTER COLUMN beat_id DROP NOT NULL;
ALTER TABLE articles ADD COLUMN category TEXT;
ALTER TABLE articles ADD COLUMN lens_source TEXT NOT NULL DEFAULT 'BEAT' CHECK (lens_source IN ('CLIENT', 'BEAT', 'TRENDING_LOCAL', 'TRENDING_GLOBAL'));
ALTER TABLE articles ADD COLUMN client_match BOOLEAN NOT NULL DEFAULT FALSE;

DROP TABLE IF EXISTS news_fetch_state CASCADE;
CREATE TABLE news_fetch_state (
    id BIGSERIAL PRIMARY KEY,
    key TEXT NOT NULL UNIQUE,
    mode TEXT NOT NULL CHECK (mode IN ('SEARCH', 'TRENDING')),
    lens_or_track TEXT NOT NULL,
    beat_id BIGINT REFERENCES beats(id) ON DELETE CASCADE,
    category TEXT,
    last_success_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    last_error_code TEXT,
    last_error_message TEXT,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    cooldown_until TIMESTAMPTZ
);

CREATE TABLE news_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key TEXT NOT NULL UNIQUE,
    payload_jsonb JSONB,
    fetched_at_utc TIMESTAMPTZ NOT NULL,
    expires_at_utc TIMESTAMPTZ NOT NULL,
    last_success_at_utc TIMESTAMPTZ,
    last_error_code TEXT,
    last_error_message TEXT
);

CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    preferred_countries TEXT[],
    preferred_langs TEXT[],
    default_sidebar_mode TEXT CHECK (default_sidebar_mode IN ('SEARCH', 'TRENDING')),
    client_lens_ratio INTEGER NOT NULL DEFAULT 40,
    trending_local_ratio INTEGER NOT NULL DEFAULT 40,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_profile_beats (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    beat_id BIGINT NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, beat_id)
);

CREATE TABLE user_keywords (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN ('CLIENT', 'COMPETITOR', 'EXCLUDE')),
    keyword TEXT NOT NULL
);

CREATE TABLE user_clients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    short_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_client_aliases (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES user_clients(id) ON DELETE CASCADE,
    alias TEXT NOT NULL
);

CREATE TABLE saved_articles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    saved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    tags TEXT[],
    PRIMARY KEY (user_id, article_id)
);

UPDATE beats SET name = 'Technology/AI' WHERE slug = 'tech-ai';
UPDATE beats SET name = 'Energy' WHERE slug = 'energy';
UPDATE beats SET name = 'Consumer/Retail' WHERE slug = 'consumer-retail';
UPDATE beats SET name = 'Finance/BFSI' WHERE slug = 'finance-bfsi';
DELETE FROM beats WHERE slug = 'public-policy-regulatory';

INSERT INTO user_profile (user_id, preferred_countries, preferred_langs, default_sidebar_mode)
SELECT id, ARRAY['us'], ARRAY['en'], 'TRENDING'
FROM users
ON CONFLICT (user_id) DO NOTHING;
