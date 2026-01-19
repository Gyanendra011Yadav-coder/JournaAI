ALTER TABLE journalist_contacts
  DROP CONSTRAINT IF EXISTS journalist_contacts_source_type_check;

ALTER TABLE journalist_contacts
  ADD CONSTRAINT journalist_contacts_source_type_check
  CHECK (source_type IN ('CSV_IMPORT', 'LICENSED_DB', 'PUBLIC_BIO', 'INFERRED', 'MANUAL', 'COMMUNITY_SHARED'));
