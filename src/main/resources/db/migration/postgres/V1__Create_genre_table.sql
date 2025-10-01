-- Enable pgcrypto for UUID generation if not already enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create table if not exists
CREATE TABLE IF NOT EXISTS genre (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_genre_name ON genre(name);
CREATE INDEX IF NOT EXISTS idx_genre_deleted_at ON genre(deleted_at);

-- Insert default genres (avoid duplicates)
INSERT INTO genre (id, name, description) VALUES
(gen_random_uuid(), 'Action', 'Movies with physical action, stunts, and fights'),
(gen_random_uuid(), 'Comedy', 'Funny movies intended to make audiences laugh'),
(gen_random_uuid(), 'Drama', 'Serious stories focusing on emotional development'),
(gen_random_uuid(), 'Horror', 'Movies designed to scare and shock viewers'),
(gen_random_uuid(), 'Sci-Fi', 'Futuristic technology, space travel, and aliens'),
(gen_random_uuid(), 'Romance', 'Love stories and emotional relationships'),
(gen_random_uuid(), 'Thriller', 'Suspense and crime-based stories'),
(gen_random_uuid(), 'Fantasy', 'Mythological or imaginative stories'),
(gen_random_uuid(), 'Biopic', 'Movies based on real-life personalities'),
(gen_random_uuid(), 'Romantic Comedy', 'Mix of love and comedy for light entertainment')
ON CONFLICT (name) DO NOTHING;
