-- Migration: add unique constraint on (user_id, poi_id) for user_poi_discoveries
ALTER TABLE user_poi_discoveries
    ADD CONSTRAINT uq_user_poi_discoveries_user_id_poi_id UNIQUE (user_id, poi_id);
