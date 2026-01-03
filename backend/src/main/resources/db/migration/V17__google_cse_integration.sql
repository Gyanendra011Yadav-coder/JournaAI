ALTER TABLE integration_settings
  ADD COLUMN IF NOT EXISTS allowed_domains TEXT;

ALTER TABLE integration_settings
  ADD COLUMN IF NOT EXISTS search_engine_id TEXT;

UPDATE integration_settings
SET provider_type = 'GOOGLE_CSE',
    default_lang = COALESCE(default_lang, 'en'),
    default_country = COALESCE(default_country, 'us'),
    max_per_request = LEAST(COALESCE(max_per_request, 5), 10),
    allowed_domains = COALESCE(allowed_domains, '*')
WHERE provider_type = 'BRAVE_SEARCH';

INSERT INTO integration_settings (
  provider_type,
  is_enabled,
  api_key_encrypted,
  default_lang,
  default_country,
  refresh_interval_minutes,
  ttl_minutes,
  max_per_request,
  allowed_domains,
  search_engine_id,
  updated_by
)
SELECT
  'GOOGLE_CSE',
  TRUE,
  NULL,
  'en',
  'us',
  30,
  15,
  5,
  '*',
  NULL,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM integration_settings WHERE provider_type = 'GOOGLE_CSE'
);
