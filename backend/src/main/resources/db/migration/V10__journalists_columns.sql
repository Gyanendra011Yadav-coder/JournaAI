DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'full_name'
  ) THEN
    ALTER TABLE journalists ADD COLUMN full_name TEXT;
  END IF;
  UPDATE journalists SET full_name = COALESCE(full_name, '') WHERE full_name IS NULL;
  ALTER TABLE journalists ALTER COLUMN full_name SET NOT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'publication_name'
  ) THEN
    ALTER TABLE journalists ADD COLUMN publication_name TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'publication_domain'
  ) THEN
    ALTER TABLE journalists ADD COLUMN publication_domain TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'linkedin'
  ) THEN
    ALTER TABLE journalists ADD COLUMN linkedin TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'designation'
  ) THEN
    ALTER TABLE journalists ADD COLUMN designation TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'beats'
  ) THEN
    ALTER TABLE journalists ADD COLUMN beats TEXT[];
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'country'
  ) THEN
    ALTER TABLE journalists ADD COLUMN country TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'city'
  ) THEN
    ALTER TABLE journalists ADD COLUMN city TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'journey_summary'
  ) THEN
    ALTER TABLE journalists ADD COLUMN journey_summary TEXT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'verification_status'
  ) THEN
    ALTER TABLE journalists ADD COLUMN verification_status TEXT;
  END IF;
  UPDATE journalists SET verification_status = COALESCE(verification_status, 'UNVERIFIED')
  WHERE verification_status IS NULL;
  ALTER TABLE journalists ALTER COLUMN verification_status SET DEFAULT 'UNVERIFIED';
  ALTER TABLE journalists ALTER COLUMN verification_status SET NOT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'completeness_score'
  ) THEN
    ALTER TABLE journalists ADD COLUMN completeness_score INTEGER;
  END IF;
  UPDATE journalists SET completeness_score = COALESCE(completeness_score, 0)
  WHERE completeness_score IS NULL;
  ALTER TABLE journalists ALTER COLUMN completeness_score SET DEFAULT 0;
  ALTER TABLE journalists ALTER COLUMN completeness_score SET NOT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'created_at'
  ) THEN
    ALTER TABLE journalists ADD COLUMN created_at TIMESTAMPTZ;
  END IF;
  UPDATE journalists SET created_at = COALESCE(created_at, NOW()) WHERE created_at IS NULL;
  ALTER TABLE journalists ALTER COLUMN created_at SET DEFAULT NOW();
  ALTER TABLE journalists ALTER COLUMN created_at SET NOT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'journalists' AND column_name = 'updated_at'
  ) THEN
    ALTER TABLE journalists ADD COLUMN updated_at TIMESTAMPTZ;
  END IF;
  UPDATE journalists SET updated_at = COALESCE(updated_at, NOW()) WHERE updated_at IS NULL;
  ALTER TABLE journalists ALTER COLUMN updated_at SET DEFAULT NOW();
  ALTER TABLE journalists ALTER COLUMN updated_at SET NOT NULL;
END $$;
