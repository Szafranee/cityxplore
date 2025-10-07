CREATE TABLE IF NOT EXISTS shared_pois
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sharer_id    UUID NOT NULL,
    recipient_id UUID NOT NULL,
    poi_id       UUID NOT NULL,
    message      TEXT,
    shared_at    TIMESTAMP        DEFAULT now(),
    viewed_at    TIMESTAMP,
    CONSTRAINT fk_shared_sharer FOREIGN KEY (sharer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_poi FOREIGN KEY (poi_id) REFERENCES points_of_interest (id) ON DELETE CASCADE
);