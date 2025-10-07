CREATE TABLE IF NOT EXISTS user_achievements
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    achievement_id UUID NOT NULL,
    achieved_at    TIMESTAMP        DEFAULT now(),
    progress_data  JSONB,
    CONSTRAINT fk_userach_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_userach_achievement FOREIGN KEY (achievement_id) REFERENCES achievements (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);