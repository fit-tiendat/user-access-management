-- USER SERVICE SCHEMA INITIALIZATION
-- This migration creates the user_profiles table for user management service

-- USER PROFILES TABLE
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,  -- References users.id from auth-service
    full_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    avatar_url TEXT,
    bio TEXT,
    date_of_birth DATE,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CREATE INDEX for faster user_id lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);

-- Note: Foreign key constraint is NOT created because auth-service and user-service 
-- use separate databases. user_id is stored as a reference only.
-- If you use a shared database, you can add:
-- ALTER TABLE user_profiles ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id);
