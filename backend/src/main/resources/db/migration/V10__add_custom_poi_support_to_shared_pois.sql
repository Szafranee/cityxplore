-- Migration: Add support for custom POI data in the shared_pois table
-- This allows users to share both existing POIs and custom POIs with friends

-- Modify the shared_pois table to support custom POI data
ALTER TABLE shared_pois
    ALTER COLUMN poi_id DROP NOT NULL,
    ADD COLUMN poi_data JSONB;

-- Add check constraint to ensure exactly one of poi_id or poi_data is provided
ALTER TABLE shared_pois
    ADD CONSTRAINT chk_poi_xor CHECK (
        (poi_id IS NOT NULL AND poi_data IS NULL) OR
        (poi_id IS NULL AND poi_data IS NOT NULL)
        );

-- Add comment explaining the XOR constraint
COMMENT ON CONSTRAINT chk_poi_xor ON shared_pois IS
    'Ensures that exactly one of poi_id (existing POI) or poi_data (custom POI) is provided';

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_shared_pois_sharer_id ON shared_pois (sharer_id);
CREATE INDEX IF NOT EXISTS idx_shared_pois_recipient_id ON shared_pois (recipient_id);
CREATE INDEX IF NOT EXISTS idx_shared_pois_recipient_viewed ON shared_pois (recipient_id, viewed_at);

-- Add comment to the poi_data column
COMMENT ON COLUMN shared_pois.poi_data IS
    'Custom POI data stored as JSONB. Contains name, description, category, location (lat/lng), and optional image URLs.';
