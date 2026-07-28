-- Auto-create databases for auth-service and user-service
-- This script runs automatically when PostgreSQL container starts for the first time

-- Create auth_service database
CREATE DATABASE auth_service;

-- Create user_service database
CREATE DATABASE user_service;

-- Grant to the administrator selected through POSTGRES_USER.
GRANT ALL PRIVILEGES ON DATABASE auth_service TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE user_service TO CURRENT_USER;
