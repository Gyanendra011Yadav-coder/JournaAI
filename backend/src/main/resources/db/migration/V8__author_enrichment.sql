CREATE TABLE IF NOT EXISTS article_author_extractions (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    author_raw TEXT,
    candidates_jsonb JSONB,
    is_non_person_author BOOLEAN NOT NULL DEFAULT FALSE,
    fetch_status TEXT NOT NULL CHECK (fetch_status IN ('SUCCESS', 'FAILED', 'BLOCKED', 'SKIPPED')),
    extracted_at TIMESTAMPTZ,
    error_code TEXT,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS journalists (
    id BIGSERIAL PRIMARY KEY,
    full_name TEXT NOT NULL,
    publication_name TEXT,
    publication_domain TEXT,
    linkedin TEXT,
    designation TEXT,
    beats TEXT[],
    country TEXT,
    city TEXT,
    journey_summary TEXT,
    verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED' CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    completeness_score INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS journalist_contacts (
    id BIGSERIAL PRIMARY KEY,
    journalist_id BIGINT NOT NULL REFERENCES journalists(id) ON DELETE CASCADE,
    email TEXT,
    phone TEXT,
    visibility TEXT NOT NULL CHECK (visibility IN ('ADMIN_ONLY', 'TEAM', 'PUBLIC')),
    source_type TEXT NOT NULL CHECK (source_type IN ('CSV_IMPORT', 'LICENSED_DB', 'PUBLIC_BIO', 'MANUAL', 'COMMUNITY_SHARED')),
    verified_at TIMESTAMPTZ,
    verified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS article_journalists (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    journalist_id BIGINT NOT NULL REFERENCES journalists(id) ON DELETE CASCADE,
    match_confidence INTEGER NOT NULL DEFAULT 0,
    match_method TEXT NOT NULL CHECK (match_method IN ('EXACT', 'FUZZY', 'ADMIN_APPROVED', 'CSV')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (article_id, journalist_id)
);

CREATE TABLE IF NOT EXISTS enrichment_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_type TEXT NOT NULL CHECK (task_type IN ('EXTRACT_AUTHOR', 'RESOLVE_JOURNALIST', 'ENRICH_PROFILE')),
    article_id BIGINT REFERENCES articles(id) ON DELETE CASCADE,
    journalist_id BIGINT REFERENCES journalists(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'NEEDS_REVIEW', 'DONE', 'FAILED', 'SKIPPED')),
    priority INTEGER NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_run_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS import_jobs (
    id BIGSERIAL PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source_name TEXT,
    summary_jsonb JSONB
);

CREATE TABLE IF NOT EXISTS import_job_rows (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('CREATED', 'UPDATED', 'CONFLICT', 'ERROR', 'SKIPPED')),
    message TEXT,
    payload_jsonb JSONB,
    journalist_id BIGINT REFERENCES journalists(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
