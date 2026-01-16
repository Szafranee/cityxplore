-- Remove the denormalised total_pois_discovered column
-- We will calculate this value dynamically using COUNT from user_poi_discoveries table
-- This ensures data consistency and removes the possibility of discrepancies

ALTER TABLE users
    DROP COLUMN IF EXISTS total_pois_discovered;
