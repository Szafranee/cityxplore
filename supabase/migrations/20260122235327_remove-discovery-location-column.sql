-- Remove the unnecessary discovery_location column from the user_poi_discoveries table

ALTER TABLE user_poi_discoveries
    DROP COLUMN discovery_location;
