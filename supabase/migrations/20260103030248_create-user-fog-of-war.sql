-- Create table for user fog of war progress
-- This table stores the revealed H3 hexagons for each user

CREATE TABLE user_fog_of_war
(
    user_id           UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    revealed_hexagons JSONB     NOT NULL DEFAULT '[]'::jsonb,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create a GIN index for efficient JSONB operations (array membership, set operations)
CREATE INDEX idx_user_fog_of_war_hexagons
    ON user_fog_of_war USING GIN (revealed_hexagons);

-- Add table comment
COMMENT ON TABLE user_fog_of_war
    IS 'Stores revealed H3 hexagons for Fog of War feature. Each user has a set of hexagon indices representing explored areas on the map.';

COMMENT ON COLUMN user_fog_of_war.revealed_hexagons
    IS 'JSONB array of H3 hexagon index strings at resolution 10. Uses GIN index for efficient set operations.';
