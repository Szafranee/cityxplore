INSERT INTO achievements ("name",
                          "description",
                          "category",
                          "criteria",
                          "icon_url",
                          "points",
                          "is_active")
VALUES
    -- 1. Centurion
    ('Centurion',
     'Discover 100 Points of Interest',
     'Exploration',
     '{
       "poi_count": 100
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '200',
     'true'),
    -- 2. Socialite
    ('Socialite',
     'Connect with 5 friends',
     'Social',
     '{
       "friend_count": 5
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '30',
     'true'),
    -- 3. Ultra Marathon
    ('Ultra Marathon',
     'Travel a total of 100km',
     'Distance',
     '{
       "distance_km": 100
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '300',
     'true'),
    -- 4. Lunch Time Explorer
    ('Lunch Time Explorer',
     'Discover a POI between 12 PM and 2 PM',
     'Time',
     '{
       "time_range": "12:00-14:00"
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '15',
     'true'),
    -- 5. Early Bird
    ('Early Bird',
     'Discover a POI between 6 AM and 9 AM',
     'Time',
     '{
       "time_range": "06:00-09:00"
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '15',
     'true'),
    -- 6. Sports Fan
    ('Sports Fan',
     'Visit 5 Sports Venues',
     'Sports',
     '{
       "category": "SPORTS",
       "poi_count": 5
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '50',
     'true'),
    -- 7. Popular
    ('Popular',
     'Connect with 20 friends',
     'Social',
     '{
       "friend_count": 20
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '100',
     'true'),
    -- 8. Urban Legend
    ('Urban Legend',
     'Discover 50 Points of Interest',
     'Exploration',
     '{
       "poi_count": 50
     }',
     'https://images.steamusercontent.com/ugc/15644782801147612239/5FCA94D26B8395124B0779FDC8526C7A46C08F32/?imw=512&&ima=fit&impolicy=Letterbox&imcolor=%23000000&letterbox=false',
     '100',
     'true'),
    -- 9. Night Owl
    ('Night Owl',
     'Discover a POI between 10 PM and 4 AM',
     'Time',
     '{
       "time_range": "22:00-04:00"
     }',
     'https://images.steamusercontent.com/ugc/15644782801147612239/5FCA94D26B8395124B0779FDC8526C7A46C08F32/?imw=512&&ima=fit&impolicy=Letterbox&imcolor=%23000000&letterbox=false',
     '20',
     'true'),
    -- 10. Landmark Hunter
    ('Landmark Hunter',
     'Discover 5 Major Landmarks',
     'Exploration',
     '{
       "is_major": true,
       "poi_count": 5
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '75',
     'true'),
    -- 11. Culture Vulture
    ('Culture Vulture',
     'Visit 5 Cultural Sites',
     'Culture',
     '{
       "category": "CULTURAL",
       "poi_count": 5
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '50',
     'true'),
    -- 12. First Steps
    ('First Steps',
     'Discover your first Point of Interest',
     'Exploration',
     '{
       "poi_count": 1
     }',
     'https://images.steamusercontent.com/ugc/15644782801147612239/5FCA94D26B8395124B0779FDC8526C7A46C08F32/?imw=512&&ima=fit&impolicy=Letterbox&imcolor=%23000000&letterbox=false',
     '10',
     'true'),
    -- 13. Tourist
    ('Tourist',
     'Discover 10 Major Landmarks',
     'Exploration',
     '{
       "is_major": true,
       "poi_count": 10
     }',
     'https://cdn.discordapp.com/attachments/618541057977876491/1458964670714085580/images.png?ex=69618e3b&is=69603cbb&hm=1007c127ba15eb31b6b4b61450c0c6312eab14d12d24a32fb0d3506f7b8d71f1',
     '150',
     'true');
