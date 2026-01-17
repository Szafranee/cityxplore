-- Add discovered_at column to shared_pois for tracking when the recipient discovers the shared POI
ALTER TABLE shared_pois
    ADD COLUMN discovered_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- Add comment explaining the column
COMMENT ON COLUMN shared_pois.discovered_at IS 'Timestamp when the recipient discovered (got close to) the shared POI location. NULL means not yet discovered.';
