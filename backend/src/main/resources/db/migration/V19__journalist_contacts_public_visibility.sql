ALTER TABLE journalist_contacts
  ALTER COLUMN visibility SET DEFAULT 'PUBLIC';

UPDATE journalist_contacts
SET visibility = 'PUBLIC'
WHERE visibility IS NULL OR visibility <> 'PUBLIC';
