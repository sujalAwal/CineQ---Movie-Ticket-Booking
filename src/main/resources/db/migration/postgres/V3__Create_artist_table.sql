CREATE TABLE IF NOT EXISTS artists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    bio TEXT,
    profile_picture TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    "order" INT DEFAULT NULL,
    industry VARCHAR(50)  NULL DEFAULT NULL,  -- Kollywood, Bollywood, Tollywood, Nepal
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_artist_name ON artists(name);
CREATE INDEX IF NOT EXISTS idx_artist_order ON artists("order");
CREATE INDEX IF NOT EXISTS idx_artist_is_active ON artists(is_active);
CREATE INDEX IF NOT EXISTS idx_artist_deleted_at ON artists(deleted_at);
CREATE INDEX IF NOT EXISTS idx_artist_industry ON artists(industry);



INSERT INTO artists (name, bio, "order", industry) VALUES
-- Bollywood
(gen_random_uuid(), 'Amitabh Bachchan', 'Legendary actor in Indian cinema.', 1, 'Bollywood'),
(gen_random_uuid(), 'Shah Rukh Khan', 'King of Bollywood, known for romantic roles.', 2, 'Bollywood'),
(gen_random_uuid(), 'Salman Khan', 'Known for action-packed roles and philanthropy.', 3, 'Bollywood'),
(gen_random_uuid(), 'Aamir Khan', 'Versatile actor with socially relevant films.', 4, 'Bollywood'),
(gen_random_uuid(), 'Akshay Kumar', 'Action and comedy roles.', 5, 'Bollywood'),
(gen_random_uuid(), 'Ranbir Kapoor', 'Known for diverse roles and strong screen presence.', 6, 'Bollywood'),
(gen_random_uuid(), 'Ranveer Singh', 'Acclaimed for energetic performances.', 7, 'Bollywood'),
(gen_random_uuid(), 'Hrithik Roshan', 'Acclaimed for dancing skills and acting.', 8, 'Bollywood'),
(gen_random_uuid(), 'Ajay Devgn', 'Known for action roles.', 9, 'Bollywood'),
(gen_random_uuid(), 'Anil Kapoor', 'Acclaimed for versatile roles.', 10, 'Bollywood'),
(gen_random_uuid(), 'Deepika Padukone', 'One of the leading actresses.', 11, 'Bollywood'),
(gen_random_uuid(), 'Priyanka Chopra', 'Bollywood actress with international fame.', 12, 'Bollywood'),
(gen_random_uuid(), 'Kareena Kapoor', 'Known for romantic and family films.', 13, 'Bollywood'),
(gen_random_uuid(), 'Alia Bhatt', 'Popular actress among youth audience.', 14, 'Bollywood'),
(gen_random_uuid(), 'Katrina Kaif', 'Versatile actress.', 15, 'Bollywood'),
(gen_random_uuid(), 'Karisma Kapoor', 'Dominated 90s Bollywood.', 16, 'Bollywood'),
(gen_random_uuid(), 'Kriti Sanon', 'Known for romantic and drama films.', 17, 'Bollywood'),
(gen_random_uuid(), 'Kangana Ranaut', 'Acclaimed for versatility and screen presence.', 18, 'Bollywood'),
(gen_random_uuid(), 'Janhvi Kapoor', 'Rising star actress.', 19, 'Bollywood'),
(gen_random_uuid(), 'Tara Sutaria', 'Known for romantic and dramatic roles.', 20, 'Bollywood'),

-- Kollywood (Tamil)
(gen_random_uuid(), 'Rajinikanth', 'Cultural icon in Tamil cinema.', 21, 'Kollywood'),
(gen_random_uuid(), 'Vijay', 'Known for action and drama films.', 22, 'Kollywood'),
(gen_random_uuid(), 'Vijay Sethupathi', 'Versatile actor in contemporary Tamil cinema.', 23, 'Kollywood'),
(gen_random_uuid(), 'Ajith Kumar', 'Known for action films.', 24, 'Kollywood'),
(gen_random_uuid(), 'Suriya', 'Acclaimed for commercial and socially relevant films.', 25, 'Kollywood'),
(gen_random_uuid(), 'Dhanush', 'Actor and singer.', 26, 'Kollywood'),
(gen_random_uuid(), 'Karthi', 'Versatile actor.', 27, 'Kollywood'),
(gen_random_uuid(), 'Silambarasan', 'Known for roles in Tamil films.', 28, 'Kollywood'),
(gen_random_uuid(), 'STR', 'Popular actor and singer.', 29, 'Kollywood'),
(gen_random_uuid(), 'Arya', 'Acclaimed actor.', 30, 'Kollywood'),
(gen_random_uuid(), 'Nayanthara', 'Leading actress in Tamil cinema.', 31, 'Kollywood'),
(gen_random_uuid(), 'Samantha Ruth Prabhu', 'Acclaimed actress.', 32, 'Kollywood'),
(gen_random_uuid(), 'Trisha Krishnan', 'Popular actress.', 33, 'Kollywood'),
(gen_random_uuid(), 'Keerthy Suresh', 'Award-winning actress.', 34, 'Kollywood'),
(gen_random_uuid(), 'Aishwarya Rajesh', 'Known for critically acclaimed roles.', 35, 'Kollywood'),
(gen_random_uuid(), 'Jyothika', 'Veteran actress.', 36, 'Kollywood'),
(gen_random_uuid(), 'Vani Bhojan', 'Rising actress.', 37, 'Kollywood'),
(gen_random_uuid(), 'Varalaxmi Sarathkumar', 'Acclaimed for action and drama.', 38, 'Kollywood'),
(gen_random_uuid(), 'Rakul Preet Singh', 'Actress in Tamil films.', 39, 'Kollywood'),
(gen_random_uuid(), 'Priya Bhavani Shankar', 'Popular actress.', 40, 'Kollywood'),

-- Tollywood (Telugu)
(gen_random_uuid(), 'Allu Arjun', 'Stylish performer and dancer.', 41, 'Tollywood'),
(gen_random_uuid(), 'Mahesh Babu', 'Top Telugu actor.', 42, 'Tollywood'),
(gen_random_uuid(), 'Prabhas', 'Known for action and fantasy films.', 43, 'Tollywood'),
(gen_random_uuid(), 'NTR Jr.', 'Leading Telugu actor.', 44, 'Tollywood'),
(gen_random_uuid(), 'Ram Charan', 'Action actor.', 45, 'Tollywood'),
(gen_random_uuid(), 'Pawan Kalyan', 'Actor and politician.', 46, 'Tollywood'),
(gen_random_uuid(), 'Chiranjeevi', 'Veteran actor.', 47, 'Tollywood'),
(gen_random_uuid(), 'Naga Chaitanya', 'Known for Telugu films.', 48, 'Tollywood'),
(gen_random_uuid(), 'Ravi Teja', 'Acclaimed for performances.', 49, 'Tollywood'),
(gen_random_uuid(), 'Allu Sirish', 'Known for roles in Telugu films.', 50, 'Tollywood'),
(gen_random_uuid(), 'Rashmika Mandanna', 'Top actress.', 51, 'Tollywood'),
(gen_random_uuid(), 'Samantha Ruth Prabhu', 'Acclaimed actress.', 52, 'Tollywood'),
(gen_random_uuid(), 'Pooja Hegde', 'Popular actress.', 53, 'Tollywood'),
(gen_random_uuid(), 'Tamannaah Bhatia', 'Versatile actress.', 54, 'Tollywood'),
(gen_random_uuid(), 'Nithya Menen', 'Acclaimed actress.', 55, 'Tollywood'),
(gen_random_uuid(), 'Keerthy Suresh', 'Award-winning actress.', 56, 'Tollywood'),
(gen_random_uuid(), 'Ileana D Cruz', 'Popular actress.', 57, 'Tollywood'),
(gen_random_uuid(), 'Anupama Parameswaran', 'Rising actress.', 58, 'Tollywood'),
(gen_random_uuid(), 'Pranitha Subhash', 'Known for Telugu films.', 59, 'Tollywood'),
(gen_random_uuid(), 'Nandita Swetha', 'Acclaimed actress.', 60, 'Tollywood'),

-- Nepali Cinema
(gen_random_uuid(), 'Rajesh Hamal', 'Maha Nayak of Nepali cinema.', 61, 'Nepal'),
(gen_random_uuid(), 'Manisha Koirala', 'Actress in Nepali and Indian films.', 62, 'Nepal'),
(gen_random_uuid(), 'Rekha Thapa', 'Actress and producer.', 63, 'Nepal'),
(gen_random_uuid(), 'Priyanka Karki', 'Popular Nepali actress.', 64, 'Nepal'),
(gen_random_uuid(), 'Niruta Singh', 'Veteran actress.', 65, 'Nepal'),
(gen_random_uuid(), 'Saugat Malla', 'Versatile actor.', 66, 'Nepal'),
(gen_random_uuid(), 'Dayahang Rai', 'Actor in theater and films.', 67, 'Nepal'),
(gen_random_uuid(), 'Barsha Raut', 'Rising star actress.', 68, 'Nepal'),
(gen_random_uuid(), 'Anmol K.C.', 'Romantic drama actor.', 69, 'Nepal'),
(gen_random_uuid(), 'Shristi Shrestha', 'Model-turned-actress.', 70, 'Nepal'),
(gen_random_uuid(), 'Swastima Khadka', 'Popular actress.', 71, 'Nepal'),
(gen_random_uuid(), 'Samragyee R.L. Shah', 'Actress in romantic and dramatic roles.', 72, 'Nepal'),
(gen_random_uuid(), 'Barsha Siwakoti', 'Actress in Nepali films.', 73, 'Nepal'),
(gen_random_uuid(), 'Malvika Subba', 'Former beauty queen turned actress.', 74, 'Nepal'),
(gen_random_uuid(), 'Keki Adhikari', 'Nepali actress.', 75, 'Nepal'),
(gen_random_uuid(), 'Nisha Adhikari', 'Actress in films and TV.', 76, 'Nepal'),
(gen_random_uuid(), 'Shree Krishna Shrestha', 'Veteran actor.', 77, 'Nepal'),
(gen_random_uuid(), 'Raj Ballav Koirala', 'Actor with versatile roles.', 78, 'Nepal'),
(gen_random_uuid(), 'Bhuwan K.C.', 'Actor, director, producer.', 79, 'Nepal'),
(gen_random_uuid(), 'Dayahang Rai', 'Acclaimed actor.', 80, 'Nepal'),
(gen_random_uuid(), 'Aanchal Sharma', 'Actress.', 81, 'Nepal'),
(gen_random_uuid(), 'Menuka Pradhan', 'Nepali actress.', 82, 'Nepal'),
(gen_random_uuid(), 'Bipin Karki', 'Actor.', 83, 'Nepal'),
(gen_random_uuid(), 'Sushil Chhetri', 'Actor.', 84, 'Nepal'),
(gen_random_uuid(), 'Shilpa Maskey', 'Actress.', 85, 'Nepal'),
(gen_random_uuid(), 'Priyanka Karki', 'Actress.', 86, 'Nepal'),
(gen_random_uuid(), 'Sushil Shrestha', 'Actor.', 87, 'Nepal'),
(gen_random_uuid(), 'Keki Adhikari', 'Actress.', 88, 'Nepal'),
(gen_random_uuid(), 'Anup Baral', 'Actor.', 89, 'Nepal'),
(gen_random_uuid(), 'Garima Panta', 'Actress.', 90, 'Nepal'),

-- Bollywood Directors
(gen_random_uuid(), 'Karan Johar', 'Famous Indian film director, producer, and TV personality.', 91, 'Bollywood'),
(gen_random_uuid(), 'Rajkumar Hirani', 'Known for blockbuster and socially relevant films.', 92, 'Bollywood'),
(gen_random_uuid(), 'Sanjay Leela Bhansali', 'Renowned director of epic and visually rich films.', 93, 'Bollywood'),
(gen_random_uuid(), 'Anurag Kashyap', 'Independent filmmaker known for dark and intense stories.', 94, 'Bollywood'),

-- Bollywood Producers
(gen_random_uuid(), 'Aditya Chopra', 'Chairman of Yash Raj Films and film producer.', 95, 'Bollywood'),
(gen_random_uuid(), 'Karan Johar Producer', 'Producer and director.', 96, 'Bollywood'),
(gen_random_uuid(), 'Rakesh Roshan', 'Producer and director, known for action films.', 97, 'Bollywood'),

-- Bollywood Music Directors
(gen_random_uuid(), 'A.R. Rahman', 'Oscar-winning music composer.', 98, 'Bollywood'),
(gen_random_uuid(), 'Pritam', 'Popular Bollywood music composer and producer.', 99, 'Bollywood'),
(gen_random_uuid(), 'Shankar-Ehsaan-Loy', 'Famous music director trio.', 100, 'Bollywood'),

-- Nepali Directors
(gen_random_uuid(), 'Deepak Rauniyar', 'Nepali filmmaker acclaimed internationally.', 101, 'Nepal'),
(gen_random_uuid(), 'Dinesh Raut', 'Director of several popular Nepali films.', 102, 'Nepal'),
(gen_random_uuid(), 'Subash Koirala', 'Well-known Nepali film director.', 103, 'Nepal'),

-- Nepali Producers
(gen_random_uuid(), 'Bhusan Dahal', 'Film producer and TV personality.', 104, 'Nepal'),
(gen_random_uuid(), 'Sunil Rawal', 'Producer of commercial Nepali films.', 105, 'Nepal'),
(gen_random_uuid(), 'Arjun Ghimire', 'Nepali film producer.', 106, 'Nepal'),

-- Nepali Music Directors
(gen_random_uuid(), 'Mingma Sherpa', 'Music composer and director in Nepali cinema.', 107, 'Nepal'),
(gen_random_uuid(), 'Sugam Pokharel', 'Popular singer and music director.', 108, 'Nepal'),
(gen_random_uuid(), 'Shiva Pariyar', 'Composer and singer.', 109, 'Nepal')

ON CONFLICT (name) DO NOTHING;
