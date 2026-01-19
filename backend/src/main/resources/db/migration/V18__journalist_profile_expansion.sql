ALTER TABLE journalists
  ADD COLUMN aliases text[],
  ADD COLUMN publication_aliases text[],
  ADD COLUMN topic_keywords text[],
  ADD COLUMN languages text[],
  ADD COLUMN coverage_regions text[],
  ADD COLUMN other_links text[];

CREATE INDEX IF NOT EXISTS idx_journalists_aliases_gin ON journalists USING GIN (aliases);
CREATE INDEX IF NOT EXISTS idx_journalists_publication_aliases_gin ON journalists USING GIN (publication_aliases);
CREATE INDEX IF NOT EXISTS idx_journalists_topic_keywords_gin ON journalists USING GIN (topic_keywords);
CREATE INDEX IF NOT EXISTS idx_journalists_languages_gin ON journalists USING GIN (languages);
CREATE INDEX IF NOT EXISTS idx_journalists_coverage_regions_gin ON journalists USING GIN (coverage_regions);
CREATE INDEX IF NOT EXISTS idx_journalists_other_links_gin ON journalists USING GIN (other_links);
