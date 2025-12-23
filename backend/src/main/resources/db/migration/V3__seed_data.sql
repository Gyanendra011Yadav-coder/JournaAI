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
