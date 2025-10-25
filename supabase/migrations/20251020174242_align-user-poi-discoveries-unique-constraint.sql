-- Align unique constraint name for user_poi_discoveries to avoid conflicts with Supabase and Hibernate
DO
$$
    BEGIN
        -- If the target constraint already exists, optionally drop legacy one to avoid duplicates
        IF EXISTS (SELECT 1
                   FROM pg_constraint c
                            JOIN pg_class t ON t.oid = c.conrelid
                   WHERE c.conname = 'uq_user_poi_discoveries_user_id_poi_id'
                     AND t.relname = 'user_poi_discoveries') THEN
            IF EXISTS (SELECT 1
                       FROM pg_constraint c
                                JOIN pg_class t ON t.oid = c.conrelid
                       WHERE c.conname = 'uq_discovery'
                         AND t.relname = 'user_poi_discoveries') THEN
                ALTER TABLE user_poi_discoveries
                    DROP CONSTRAINT uq_discovery;
            END IF;
        ELSE
            -- If the older constraint name exists, rename it to the target name
            IF EXISTS (SELECT 1
                       FROM pg_constraint c
                                JOIN pg_class t ON t.oid = c.conrelid
                       WHERE c.conname = 'uq_discovery'
                         AND t.relname = 'user_poi_discoveries') THEN
                ALTER TABLE user_poi_discoveries
                    RENAME CONSTRAINT uq_discovery TO uq_user_poi_discoveries_user_id_poi_id;
            ELSE
                -- Otherwise, add the target constraint explicitly
                ALTER TABLE user_poi_discoveries
                    ADD CONSTRAINT uq_user_poi_discoveries_user_id_poi_id UNIQUE (user_id, poi_id);
            END IF;
        END IF;
    END
$$ LANGUAGE plpgsql;
