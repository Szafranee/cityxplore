-- Add a new column 'total_achievement_points' to the 'users' table
ALTER TABLE users
    ADD COLUMN total_achievement_points INT DEFAULT 0;

-- Initialise existing records to have 0 achievement points
UPDATE users
SET total_achievement_points = 0
WHERE total_achievement_points IS NULL;
