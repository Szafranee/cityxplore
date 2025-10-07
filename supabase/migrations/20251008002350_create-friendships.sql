CREATE TABLE IF NOT EXISTS friendships
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID        NOT NULL,
    addressee_id UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL, -- pending, accepted, blocked
    created_at   TIMESTAMP        DEFAULT now(),
    updated_at   TIMESTAMP        DEFAULT now(),
    CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friendship_addressee FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_friendship UNIQUE (requester_id, addressee_id),
    CONSTRAINT ck_friendship_self CHECK (requester_id <> addressee_id)
);