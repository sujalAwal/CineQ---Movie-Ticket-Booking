-- Enable pgcrypto for UUID generation if not already enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create artist_role_types table
CREATE TABLE IF NOT EXISTS artist_role_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    "order" INT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_role_type_name ON artist_role_types(name);
CREATE INDEX IF NOT EXISTS idx_role_type_order ON artist_role_types("order");
CREATE INDEX IF NOT EXISTS idx_role_type_is_active ON artist_role_types(is_active);
CREATE INDEX IF NOT EXISTS idx_role_type_deleted_at ON artist_role_types(deleted_at);

-- Insert default role types
INSERT INTO artist_role_types (name, "order") VALUES
('Director', 1),
('Producer', 2),
('Actor', 3),
('Actress', 4),
('Music Director', 5),
('Cinematographer', 6),
('Editor', 7),
('Writer', 8),
('Screenwriter', 9),
('Lyricist', 10)
ON CONFLICT (name) DO NOTHING;