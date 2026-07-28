-- V2 Migration: Add optional contact information to profiles
-- This demonstrates schema evolution for user-service

-- Add address for user location tracking
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS address VARCHAR(255);

-- Add phone for contact information
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

-- Add date of birth for age verification
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS date_of_birth DATE;

-- Create index on phone for faster searches
CREATE INDEX IF NOT EXISTS idx_profiles_phone ON profiles(phone);

-- Optional: Add comments to document the columns
COMMENT ON COLUMN profiles.address IS 'User residential address';
COMMENT ON COLUMN profiles.phone IS 'User contact phone number';
COMMENT ON COLUMN profiles.date_of_birth IS 'User date of birth for age verification';
