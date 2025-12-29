-- Migration: Add is_major flag for points_of_interest table
-- This flag indicates whether a point of interest is considered a major landmark (most important)

ALTER TABLE points_of_interest
    ADD COLUMN is_major BOOLEAN NOT NULL DEFAULT FALSE;
