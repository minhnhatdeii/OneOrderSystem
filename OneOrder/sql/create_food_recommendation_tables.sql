-- 1. Create tables for Food Recommendation Feature

CREATE TABLE IF NOT EXISTS food_posts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID, -- References tenants(id) from OneOrder_SM
    menu_item_id BIGINT REFERENCES menu_items(id) ON DELETE CASCADE,
    images JSONB NOT NULL DEFAULT '[]', -- List of {url, layout}
    caption TEXT,
    category_tags TEXT[] DEFAULT '{}', -- Populated by Gemini 1.5
    like_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    share_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    is_trending BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Note: tenants might not have a strong FK here if created in another script, but tenant_id is UUID.

CREATE TABLE IF NOT EXISTS food_post_likes (
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    post_id UUID REFERENCES food_posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE IF NOT EXISTS food_post_comments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    post_id UUID REFERENCES food_posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS food_post_shares (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    post_id UUID REFERENCES food_posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    platform TEXT, -- e.g., 'facebook', 'zalo', 'copy_link'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS food_post_views (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    post_id UUID REFERENCES food_posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE, -- NULL if anonymous visitor
    duration_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);


-- 2. Setup RLS (Row Level Security)

ALTER TABLE food_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE food_post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE food_post_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE food_post_shares ENABLE ROW LEVEL SECURITY;
ALTER TABLE food_post_views ENABLE ROW LEVEL SECURITY;

-- food_posts: Anyone can read, only staff/manager can write (assuming public read for now)
DROP POLICY IF EXISTS "Public can view food posts" ON food_posts;
CREATE POLICY "Public can view food posts" ON food_posts FOR SELECT USING (true);

-- Likes: Users can insert/delete their own likes, anyone can view
DROP POLICY IF EXISTS "Public can view likes" ON food_post_likes;
CREATE POLICY "Public can view likes" ON food_post_likes FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can manage their own likes" ON food_post_likes;
CREATE POLICY "Users can manage their own likes" ON food_post_likes FOR ALL USING (auth.uid() = user_id);

-- Comments: Users can insert/delete their own comments, anyone can view
DROP POLICY IF EXISTS "Public can view comments" ON food_post_comments;
CREATE POLICY "Public can view comments" ON food_post_comments FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can manage their own comments" ON food_post_comments;
CREATE POLICY "Users can manage their own comments" ON food_post_comments FOR ALL USING (auth.uid() = user_id);

-- Shares & Views: Anyone can view stats, users can insert
DROP POLICY IF EXISTS "Users can insert shares" ON food_post_shares;
CREATE POLICY "Users can insert shares" ON food_post_shares FOR INSERT WITH CHECK (auth.uid() = user_id OR user_id IS NULL);
DROP POLICY IF EXISTS "Users can insert views" ON food_post_views;
CREATE POLICY "Users can insert views" ON food_post_views FOR INSERT WITH CHECK (auth.uid() = user_id OR user_id IS NULL);


-- 3. Triggers for maintaining counters

CREATE OR REPLACE FUNCTION increment_like_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE food_posts SET like_count = like_count + 1 WHERE id = NEW.post_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION decrement_like_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE food_posts SET like_count = like_count - 1 WHERE id = OLD.post_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_like_added ON food_post_likes;
CREATE TRIGGER on_like_added
    AFTER INSERT ON food_post_likes
    FOR EACH ROW EXECUTE FUNCTION increment_like_count();

DROP TRIGGER IF EXISTS on_like_removed ON food_post_likes;
CREATE TRIGGER on_like_removed
    AFTER DELETE ON food_post_likes
    FOR EACH ROW EXECUTE FUNCTION decrement_like_count();

-- Similar trigger for comments
CREATE OR REPLACE FUNCTION increment_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE food_posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_comment_added ON food_post_comments;
CREATE TRIGGER on_comment_added
    AFTER INSERT ON food_post_comments
    FOR EACH ROW EXECUTE FUNCTION increment_comment_count();


-- 4. Haversine Distance Function
-- Returns distance in Kilometers
CREATE OR REPLACE FUNCTION calculate_distance_km(lat1 FLOAT, lon1 FLOAT, lat2 FLOAT, lon2 FLOAT)
RETURNS FLOAT AS $$
DECLARE
    R FLOAT := 6371; -- Earth radius in km
    dLat FLOAT;
    dLon FLOAT;
    a FLOAT;
    c FLOAT;
    d FLOAT;
BEGIN
    IF lat1 IS NULL OR lon1 IS NULL OR lat2 IS NULL OR lon2 IS NULL THEN
        RETURN 9999999; -- Large distance if location is unknown
    END IF;

    dLat := radians(lat2 - lat1);
    dLon := radians(lon2 - lon1);

    a := sin(dLat/2) * sin(dLat/2) +
         cos(radians(lat1)) * cos(radians(lat2)) *
         sin(dLon/2) * sin(dLon/2);
    
    c := 2 * atan2(sqrt(a), sqrt(1-a));
    d := R * c;

    RETURN d;
END;
$$ LANGUAGE plpgsql IMMUTABLE;


-- 5. Hybrid Recommendation Engine Function
-- NOTE: Requires 'restaurants' table to have lat/lng or passed somehow. Using 'tenants' schema.
-- Since tenants table might not have lat/lng yet natively, we will assume tenants currently has them or we'll mock them.
-- For the sake of the engine:

CREATE OR REPLACE FUNCTION get_food_recommendations(
    p_user_id UUID, 
    p_user_lat FLOAT, 
    p_user_lng FLOAT,
    p_limit INTEGER DEFAULT 50
)
RETURNS TABLE (
    post_id UUID,
    score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH UserInteractions AS (
        -- Get tags the user has interacted with (Liked or Commented)
        SELECT unnest(fp.category_tags) as tag, COUNT(*) as tag_weight
        FROM food_posts fp
        LEFT JOIN food_post_likes fpl ON fpl.post_id = fp.id
        LEFT JOIN food_post_views fpv ON fpv.post_id = fp.id AND fpv.duration_seconds > 5
        WHERE fpl.user_id = p_user_id OR fpv.user_id = p_user_id
        GROUP BY tag
    ),
    ScoredPosts AS (
        SELECT 
            fp.id,
            -- Content Score (0.55): Matches user's liked tags
            (
                SELECT COALESCE(SUM(tag_weight), 0) / NULLIF((SELECT MAX(tag_weight) FROM UserInteractions), 0)
                FROM UserInteractions ui
                WHERE ui.tag = ANY(fp.category_tags)
            ) * 0.55 AS content_score,
            
            -- Location Score (0.35): Distance. Assuming tenant location is joined
            (
                CASE WHEN (p_user_lat = 0 AND p_user_lng = 0) THEN 0 
                ELSE (1 - LEAST(calculate_distance_km(p_user_lat, p_user_lng, 10.7626, 106.6601) / 5.0, 1)) * 0.35 
                END
                -- Note: using hardcoded tenant location 10.7626, 106.6601 for MVP unless tenant table has lat/lng
            ) AS location_score,
            
            -- Trending (0.0): Number of likes / views (Removed from priority based on tuning)
            0.0 AS trending_score,
            
            -- Freshness (0.10): Age of post
            GREATEST(1 - EXTRACT(EPOCH FROM (NOW() - fp.created_at)) / 2592000, 0) * 0.10 AS freshness_score
            
            -- Note: Collaborative Filtering is omitted from this base SQL and replaced by a simpler Content query due to SQL complexity.
            -- Full CF requires mapping user similarities.

        FROM food_posts fp
    )
    SELECT 
        id, 
        COALESCE(content_score, 0) + COALESCE(location_score, 0) + COALESCE(trending_score, 0) + COALESCE(freshness_score, 0) AS final_score
    FROM ScoredPosts
    ORDER BY final_score DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

