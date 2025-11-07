-- Add a new column 'total_achievement_points' to the 'users' table
ALTER TABLE users
    ADD COLUMN total_achievement_points INT DEFAULT 0 NOT NULL;
