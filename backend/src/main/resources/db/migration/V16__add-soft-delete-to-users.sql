-- Add deleted_at column for soft delete support
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;

-- Comment for documentation
COMMENT ON COLUMN users.deleted_at IS 'Timestamp when the user account was soft-deleted. If null, the user is active.';
