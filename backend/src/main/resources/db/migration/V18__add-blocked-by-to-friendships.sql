-- Add blocked_by column to friendships table
-- This column tracks which user blocked the friendship (null if not blocked)

ALTER TABLE friendships
    ADD COLUMN blocked_by UUID REFERENCES users (id);

-- Add comment
COMMENT ON COLUMN friendships.blocked_by IS 'ID of the user who blocked the friendship (null if status != BLOCKED)';

-- Create index for better performance on blocked queries
CREATE INDEX idx_friendships_blocked_by ON friendships (blocked_by) WHERE blocked_by IS NOT NULL;
