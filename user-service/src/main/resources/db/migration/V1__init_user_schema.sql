-- USER SERVICE SCHEMA INITIALIZATION
-- The application currently links a profile to auth-service by JWT username.
-- Keep this migration aligned with com.r2s.user.entity.Profile.

CREATE TABLE IF NOT EXISTS profiles (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_email ON profiles(email);

-- There is intentionally no foreign key to auth-service because each service
-- owns a separate PostgreSQL database. A future version should prefer an
-- immutable auth user ID over username for this cross-service reference.
