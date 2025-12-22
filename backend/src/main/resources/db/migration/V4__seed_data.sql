INSERT INTO beats (name, slug) VALUES
  ('Law', 'law'),
  ('Taxation', 'taxation'),
  ('Healthcare', 'healthcare'),
  ('Education', 'education'),
  ('Steel', 'steel'),
  ('Finance/BFSI', 'finance-bfsi'),
  ('Startups/VC', 'startups-vc'),
  ('Technology/AI', 'technology-ai'),
  ('Telecom', 'telecom'),
  ('Energy & Renewables', 'energy-renewables'),
  ('Manufacturing/Industrial', 'manufacturing-industrial'),
  ('Pharma/MedTech', 'pharma-medtech'),
  ('Real Estate & Infra', 'real-estate-infra'),
  ('Retail/Consumer', 'retail-consumer'),
  ('Auto/EV', 'auto-ev'),
  ('ESG/Sustainability', 'esg-sustainability'),
  ('Public Policy/Regulatory', 'public-policy-regulatory');

INSERT INTO outreach_templates (name, subject, body) VALUES
  ('Breaking news hook', 'Story idea on {{beat}}: {{headline}}',
   'Hi {{journalist_name}},\n\nI noticed your coverage at {{outlet}}. We have a data-backed perspective on {{beat}} that ties directly to this story: {{article_link}}.\n\nClient quote: "{{client_quote}}"\n\nWould you like a quick briefing?\n\nBest,\nPR News & Outreach Team'),
  ('Rapid response', 'Quick comment for {{outlet}} on {{headline}}',
   'Hello {{journalist_name}},\n\nWe can provide a rapid response for {{outlet}} on {{headline}}. Our spokesperson is available today.\n\nKey point: {{client_quote}}\n\nHappy to coordinate.\n\nThanks,\nPR News & Outreach Team'),
  ('Trend pitch', 'Trend briefing: {{beat}} in focus',
   'Hi {{journalist_name}},\n\nWe are seeing momentum across {{beat}} and can share a trend brief tailored for {{outlet}}.\n\nRelevant link: {{article_link}}\n\nQuote: "{{client_quote}}"\n\nOpen to a quick call?\n\nRegards,\nPR News & Outreach Team'),
  ('Executive availability', 'Interview availability: {{beat}} leader',
   'Hi {{journalist_name}},\n\nWe can offer an interview with our {{beat}} lead. Your recent piece caught our attention: {{article_link}}.\n\nSample quote: "{{client_quote}}"\n\nLet me know if this is useful.\n\nBest,\nPR News & Outreach Team'),
  ('Data drop', 'Fresh data for {{outlet}} on {{beat}}',
   'Hi {{journalist_name}},\n\nWe have new survey insights related to {{beat}} that could complement your coverage at {{outlet}}.\n\nLink: {{article_link}}\n\nQuote: "{{client_quote}}"\n\nWould you like the deck?\n\nBest,\nPR News & Outreach Team');

INSERT INTO articles (headline, source, author, canonical_url, url, published_at, summary, provider, raw_payload)
SELECT
  'Seeded article ' || gs,
  'Outlet ' || ((gs % 20) + 1),
  'Reporter ' || ((gs % 15) + 1),
  'https://news.example.com/article-' || gs,
  'https://news.example.com/article-' || gs,
  NOW() - ((gs % 30) || ' hours')::interval,
  'This is a seeded summary for article ' || gs || '. It highlights key points for PR teams.',
  'seed',
  '{}'::jsonb
FROM generate_series(1, 100) AS gs;

INSERT INTO article_tags (article_id, beat_id)
SELECT id, ((id - 1) % 17) + 1
FROM articles;

INSERT INTO journalists (name, outlet, location, email, phone, source_provider, provider_reference_id)
SELECT
  'Journalist ' || gs,
  'Outlet ' || ((gs % 30) + 1),
  CASE WHEN gs % 4 = 0 THEN 'New York, NY'
       WHEN gs % 4 = 1 THEN 'London, UK'
       WHEN gs % 4 = 2 THEN 'Singapore'
       ELSE 'Bengaluru, IN' END,
  'journalist' || gs || '@example.com',
  '+1-555-01' || LPAD(gs::text, 3, '0'),
  'seed',
  'seed-' || gs
FROM generate_series(1, 200) AS gs;

INSERT INTO journalist_tags (journalist_id, beat_id)
SELECT id, ((id - 1) % 17) + 1
FROM journalists;
