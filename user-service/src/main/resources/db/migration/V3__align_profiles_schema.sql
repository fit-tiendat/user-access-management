-- Align the Flyway-managed schema with the Profile JPA entity.
-- Preserve the obsolete table for manual data reconciliation instead of dropping it.
DO $$
BEGIN
    IF to_regclass('public.user_profiles') IS NOT NULL
       AND to_regclass('public.legacy_user_profiles') IS NULL THEN
        ALTER TABLE user_profiles RENAME TO legacy_user_profiles;
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS profiles (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) NOT NULL,
    CONSTRAINT uk_profiles_username UNIQUE (username),
    CONSTRAINT uk_profiles_email UNIQUE (email)
);

COMMENT ON TABLE profiles IS 'User profiles keyed by the username issued by auth-service';
COMMENT ON TABLE legacy_user_profiles IS
    'Legacy profile schema retained for manual reconciliation after V3';
