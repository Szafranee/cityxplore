create extension if not exists "pgcrypto";

CREATE TABLE IF NOT EXISTS users
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) UNIQUE NOT NULL,
    username              VARCHAR(50) UNIQUE  NOT NULL,
    avatar_url            VARCHAR(500),
    created_at            TIMESTAMP        DEFAULT now(),
    last_active_at        TIMESTAMP,
    total_distance        DECIMAL(10, 2)   DEFAULT 0,
    total_pois_discovered INT              DEFAULT 0
);
