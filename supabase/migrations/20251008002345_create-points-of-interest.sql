CREATE TABLE IF NOT EXISTS points_of_interest
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(50)  NOT NULL,
    location    GEOGRAPHY(POINT, 4326),
    metadata    JSONB,
    image_urls  JSONB,
    created_at  TIMESTAMP        DEFAULT now(),
    updated_at  TIMESTAMP        DEFAULT now(),
    is_active   BOOLEAN          DEFAULT TRUE
);