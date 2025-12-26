ALTER TABLE user_profile
  DROP CONSTRAINT IF EXISTS user_profile_default_sidebar_mode_check;

ALTER TABLE user_profile
  ADD CONSTRAINT user_profile_default_sidebar_mode_check
  CHECK (default_sidebar_mode IN ('SEARCH', 'TRENDING', 'DASHBOARD'));
