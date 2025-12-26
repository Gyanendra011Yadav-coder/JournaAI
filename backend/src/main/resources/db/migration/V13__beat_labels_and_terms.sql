UPDATE beats
SET name = 'Entertainment',
    slug = 'entertainment'
WHERE slug = 'esg' OR name = 'ESG';

UPDATE beats
SET name = 'Finance & Banking',
    slug = 'finance-banking'
WHERE slug = 'finance-bfsi' OR name = 'Finance/BFSI';

UPDATE beats
SET name = 'Technology & AI',
    slug = 'technology-ai'
WHERE slug = 'tech-ai' OR name IN ('Tech/AI', 'Technology/AI');

UPDATE beats
SET name = 'Startups & VC'
WHERE slug = 'startups-vc' OR name = 'Startups/VC';

UPDATE beats
SET name = 'Pharma & MedTech'
WHERE slug = 'pharma-medtech' OR name = 'Pharma/MedTech';

UPDATE beats
SET name = 'Real Estate & Infrastructure'
WHERE slug = 'real-estate-infra' OR name = 'Real Estate & Infra';

UPDATE beats
SET name = 'Consumer & Retail'
WHERE slug = 'consumer-retail' OR name = 'Consumer/Retail';

UPDATE beats
SET name = 'Automotive & EV'
WHERE slug = 'auto-ev' OR name = 'Auto/EV';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["startup funding","venture capital"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'startups-vc';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["real estate","infrastructure projects"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'real-estate-infra';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["pharmaceutical industry","medical technology"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'pharma-medtech';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["automotive industry","electric vehicle"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'auto-ev';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["technology sector","artificial intelligence"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'technology-ai';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["consumer goods","retail industry"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'consumer-retail';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["financial services","investment banking"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'finance-banking';

UPDATE beat_query_templates template
SET beat_terms_jsonb = '["entertainment industry","streaming media"]'::jsonb
FROM beats beat
WHERE template.beat_id = beat.id
  AND beat.slug = 'entertainment';
