DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'name'
  ) THEN
    UPDATE journalists
    SET full_name = COALESCE(NULLIF(full_name, ''), name)
    WHERE name IS NOT NULL;
    ALTER TABLE journalists DROP COLUMN name;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'outlet'
  ) THEN
    UPDATE journalists
    SET publication_name = COALESCE(publication_name, outlet)
    WHERE outlet IS NOT NULL;
    ALTER TABLE journalists DROP COLUMN outlet;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'beat_tags'
  ) THEN
    UPDATE journalists
    SET beats = COALESCE(beats, string_to_array(beat_tags, ';'))
    WHERE beat_tags IS NOT NULL;
    ALTER TABLE journalists DROP COLUMN beat_tags;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'location'
  ) THEN
    UPDATE journalists
    SET city = COALESCE(city, location)
    WHERE location IS NOT NULL;
    ALTER TABLE journalists DROP COLUMN location;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'email'
  ) THEN
    ALTER TABLE journalists DROP COLUMN email;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'phone'
  ) THEN
    ALTER TABLE journalists DROP COLUMN phone;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'source_provider'
  ) THEN
    ALTER TABLE journalists DROP COLUMN source_provider;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'provider_reference_id'
  ) THEN
    ALTER TABLE journalists DROP COLUMN provider_reference_id;
  END IF;
END $$;
