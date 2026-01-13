-- Convert total_pois_discovered to a GENERATED column (computed column)
-- This ensures the value is always accurate and automatically updated by PostgreSQL

ALTER TABLE users
    DROP COLUMN IF EXISTS total_pois_discovered;

ALTER TABLE users
    ADD COLUMN total_pois_discovered INTEGER GENERATED ALWAYS AS (
        (SELECT COUNT(id)::INTEGER
         FROM user_poi_discoveries
         WHERE user_id = users.id)
        ) STORED;

CREATE INDEX IF NOT EXISTS idx_users_total_pois_discovered ON users (total_pois_discovered);
