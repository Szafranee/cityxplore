-- Trigger to sync email updates from auth.users to public.users
CREATE OR REPLACE FUNCTION public.handle_user_email_update()
    RETURNS TRIGGER
    SECURITY DEFINER
    SET search_path = public, pg_catalog -- Lock down search path for security
AS
$$
DECLARE
    v_user_exists BOOLEAN;
BEGIN
    -- Check if user exists to prevent errors
    SELECT EXISTS (SELECT 1
                   FROM public.users
                   WHERE id = NEW.id)
    INTO v_user_exists;

    IF NOT v_user_exists THEN
        -- Log notice but don't fail, as public.users might lag behind auth.users created elsewhere
        RAISE NOTICE 'User with ID % does not exist in public.users, skipping email sync.', NEW.id;
        RETURN NEW;
    END IF;

    BEGIN
        UPDATE public.users
        SET email = NEW.email
        WHERE id = NEW.id;

        IF FOUND THEN
            RAISE NOTICE 'Successfully synced email for user %', NEW.id;
        ELSE
            RAISE WARNING 'Update failed for user % (row not found despite check)', NEW.id;
        END IF;
    EXCEPTION
        WHEN unique_violation THEN
            -- Raise exception to prevent auth and public email desync
            RAISE EXCEPTION 'Email sync failed: Email % is already in use by another user.', NEW.email;
    END;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Use drop trigger if exists to make it idempotent
DROP TRIGGER IF EXISTS on_auth_user_email_updated ON auth.users;

CREATE TRIGGER on_auth_user_email_updated
    AFTER UPDATE
    ON auth.users
    FOR EACH ROW
    WHEN (OLD.email IS DISTINCT FROM NEW.email)
EXECUTE FUNCTION public.handle_user_email_update();
