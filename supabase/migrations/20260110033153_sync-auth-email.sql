-- Trigger to sync email updates from auth.users to public.users
create or replace function public.handle_user_email_update()
    returns trigger as
$$
begin
    update public.users
    set email = new.email
    where id = new.id;
    return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_email_updated
    after update
    on auth.users
    for each row
    when (old.email is distinct from new.email)
execute procedure public.handle_user_email_update();
