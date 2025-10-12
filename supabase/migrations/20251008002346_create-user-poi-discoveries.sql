CREATE TABLE IF NOT EXISTS user_poi_discoveries
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    poi_id             UUID NOT NULL,
    discovered_at      TIMESTAMP        DEFAULT now(),
    discovery_location GEOGRAPHY(POINT, 4326),
    is_favorite        BOOLEAN          DEFAULT FALSE,
    CONSTRAINT fk_discovery_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_discovery_poi FOREIGN KEY (poi_id) REFERENCES points_of_interest (id) ON DELETE CASCADE,
    CONSTRAINT uq_discovery UNIQUE (user_id, poi_id)
);