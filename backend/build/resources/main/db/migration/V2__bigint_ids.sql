-- Align database key types with JPA Long identifiers.
ALTER TABLE users DROP CONSTRAINT users_workspace_id_fkey;
ALTER TABLE saved_searches DROP CONSTRAINT saved_searches_beat_id_fkey;
ALTER TABLE outreach_emails DROP CONSTRAINT outreach_emails_article_id_fkey;
ALTER TABLE outreach_emails DROP CONSTRAINT outreach_emails_journalist_id_fkey;
ALTER TABLE outreach_emails DROP CONSTRAINT outreach_emails_template_id_fkey;

ALTER TABLE workspaces ALTER COLUMN id TYPE BIGINT;
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
ALTER TABLE beats ALTER COLUMN id TYPE BIGINT;
ALTER TABLE saved_searches ALTER COLUMN id TYPE BIGINT;
ALTER TABLE articles ALTER COLUMN id TYPE BIGINT;
ALTER TABLE journalists ALTER COLUMN id TYPE BIGINT;
ALTER TABLE outreach_templates ALTER COLUMN id TYPE BIGINT;
ALTER TABLE outreach_emails ALTER COLUMN id TYPE BIGINT;
ALTER TABLE audit_log ALTER COLUMN id TYPE BIGINT;

ALTER TABLE users ALTER COLUMN workspace_id TYPE BIGINT;
ALTER TABLE saved_searches ALTER COLUMN beat_id TYPE BIGINT;
ALTER TABLE outreach_emails ALTER COLUMN article_id TYPE BIGINT;
ALTER TABLE outreach_emails ALTER COLUMN journalist_id TYPE BIGINT;
ALTER TABLE outreach_emails ALTER COLUMN template_id TYPE BIGINT;

ALTER SEQUENCE workspaces_id_seq AS BIGINT;
ALTER SEQUENCE users_id_seq AS BIGINT;
ALTER SEQUENCE beats_id_seq AS BIGINT;
ALTER SEQUENCE saved_searches_id_seq AS BIGINT;
ALTER SEQUENCE articles_id_seq AS BIGINT;
ALTER SEQUENCE journalists_id_seq AS BIGINT;
ALTER SEQUENCE outreach_templates_id_seq AS BIGINT;
ALTER SEQUENCE outreach_emails_id_seq AS BIGINT;
ALTER SEQUENCE audit_log_id_seq AS BIGINT;

ALTER TABLE users
    ADD CONSTRAINT users_workspace_id_fkey
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
ALTER TABLE saved_searches
    ADD CONSTRAINT saved_searches_beat_id_fkey
        FOREIGN KEY (beat_id) REFERENCES beats(id);
ALTER TABLE outreach_emails
    ADD CONSTRAINT outreach_emails_article_id_fkey
        FOREIGN KEY (article_id) REFERENCES articles(id);
ALTER TABLE outreach_emails
    ADD CONSTRAINT outreach_emails_journalist_id_fkey
        FOREIGN KEY (journalist_id) REFERENCES journalists(id);
ALTER TABLE outreach_emails
    ADD CONSTRAINT outreach_emails_template_id_fkey
        FOREIGN KEY (template_id) REFERENCES outreach_templates(id);
