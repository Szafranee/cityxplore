CREATE TABLE IF NOT EXISTS achievements
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    category    VARCHAR(50),
    criteria    JSONB        NOT NULL,
    icon_url    VARCHAR(500),
    points      INT              DEFAULT 0,
    is_active   BOOLEAN          DEFAULT TRUE
);