CREATE TABLE workspaces (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    workspace_id INTEGER REFERENCES workspaces(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE beats (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE saved_searches (
    id SERIAL PRIMARY KEY,
    beat_id INTEGER REFERENCES beats(id),
    timeframe TEXT NOT NULL,
    filters JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    headline TEXT NOT NULL,
    source TEXT,
    author TEXT,
    url TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    summary TEXT,
    raw_payload JSONB,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    saved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE journalists (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    outlet TEXT,
    beat_tags TEXT,
    location TEXT,
    email TEXT,
    phone TEXT,
    source_provider TEXT NOT NULL,
    provider_reference_id TEXT NOT NULL
);

CREATE TABLE outreach_templates (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    subject TEXT NOT NULL,
    body TEXT NOT NULL
);

CREATE TABLE outreach_emails (
    id SERIAL PRIMARY KEY,
    article_id INTEGER REFERENCES articles(id),
    journalist_id INTEGER REFERENCES journalists(id),
    template_id INTEGER REFERENCES outreach_templates(id),
    final_subject TEXT NOT NULL,
    final_body TEXT NOT NULL,
    status TEXT NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    provider_message_id TEXT
);

CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    entity TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata JSONB
);
