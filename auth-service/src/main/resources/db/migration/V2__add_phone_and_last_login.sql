-- V2 Migration: Add phone number and last login tracking to users table
-- This demonstrates how to add new columns to existing tables

-- Add phone column for two-factor authentication
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

-- Add last_login timestamp to track user activity
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;

-- Create index on phone for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- Create index on last_login for activity monitoring
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login);

-- Optional: Add comment to document the columns
COMMENT ON COLUMN users.phone IS 'User phone number for 2FA and notifications';
COMMENT ON COLUMN users.last_login IS 'Timestamp of user last successful login';
