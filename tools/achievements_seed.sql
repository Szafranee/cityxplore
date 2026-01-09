-- Insert Achievements
WITH inserted_achievements AS (
    INSERT INTO public.achievements (name, description, category, criteria, icon_url, points, is_active)
        VALUES ('First Steps', 'Discover your first Point of Interest', 'Exploration',
                '{
                  "count": 1
                }'::jsonb,
                'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
                10,
                true),

               ('Urban Legend', 'Discover 50 Points of Interest', 'Exploration',
                '{
                  "count": 50
                }'::jsonb,
                'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
                100, true),

               ('Park Ranger', 'Visit 5 Parks', 'Nature',
                '{
                  "category": "Park",
                  "count": 5
                }'::jsonb,
                'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
                25,
                true),

               ('History Buff', 'Visit 10 Historical sites', 'History',
                '{
                  "category": "Historical",
                  "count": 10
                }'::jsonb,
                'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
                50,
                true),

               ('Night Owl', 'Discover a POI between 10 PM and 4 AM', 'Time',
                '{
                  "time_range": "22:00-04:00"
                }'::jsonb,
                'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
                20,
                true),

               ('Marathoner', 'Travel a total of 42km', 'Distance',
                '{
                  "distance_km": 42
                }'::jsonb,
                'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
                200, true)
        RETURNING id, name)
-- Insert User Achievements (linking specific achievements to the user)
INSERT
INTO public.user_achievements (user_id, achievement_id, achieved_at, progress_data)
SELECT '46f0ab7d-c4b5-4e94-80c2-a85066c2be11'::uuid,
       id,
       NOW() - (random() * interval '30 days'),
       '{
         "completed": true
       }'::jsonb
FROM inserted_achievements
WHERE name IN ('First Steps', 'Park Ranger', 'History Buff');


INSERT INTO achievements (name, description, category, criteria, icon_url, points, is_active)
VALUES ('Social Butterfly', 'Connect with 10 friends', 'Social',
        '{
          "friend_count": 10
        }'::jsonb,
        'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
        30,
        true),

       ('Globetrotter', 'Visit Points of Interest in 5 different countries', 'Exploration',
        '{
          "countries_visited": 5
        }'::jsonb,
        'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
        150,
        true);
