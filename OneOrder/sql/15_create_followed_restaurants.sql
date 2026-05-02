-- Create followed_restaurants table for OneOrder (customer-side following restaurants)
-- This table tracks which users follow which restaurant tenants

CREATE TABLE IF NOT EXISTS followed_restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Prevent duplicate follows
    UNIQUE(user_id, tenant_id)
);

-- Index for fast lookup by user
CREATE INDEX IF NOT EXISTS idx_followed_restaurants_user_id ON followed_restaurants(user_id);

-- Index for fast lookup by tenant (for follower count)
CREATE INDEX IF NOT EXISTS idx_followed_restaurants_tenant_id ON followed_restaurants(tenant_id);

-- Index for checking if user follows a specific restaurant
CREATE UNIQUE INDEX IF NOT EXISTS idx_followed_restaurants_unique_user_tenant ON followed_restaurants(user_id, tenant_id);

-- RLS: Users can only see/manage their own follows
ALTER TABLE followed_restaurants ENABLE ROW LEVEL SECURITY;

-- Users can read their own follows
CREATE POLICY "Users can view their own followed restaurants"
    ON followed_restaurants FOR SELECT
    USING (auth.uid() = user_id);

-- Users can insert their own follows
CREATE POLICY "Users can follow a restaurant"
    ON followed_restaurants FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Users can delete their own follows (unfollow)
CREATE POLICY "Users can unfollow a restaurant"
    ON followed_restaurants FOR DELETE
    USING (auth.uid() = user_id);

-- Function to follow a restaurant (safe, idempotent)
CREATE OR REPLACE FUNCTION follow_restaurant(p_tenant_id UUID)
RETURNS UUID AS $$
DECLARE
    v_follow_id UUID;
BEGIN
    INSERT INTO followed_restaurants (user_id, tenant_id)
    VALUES (auth.uid(), p_tenant_id)
    ON CONFLICT (user_id, tenant_id) DO NOTHING
    RETURNING id INTO v_follow_id;

    -- Update followers_count in restaurant_profiles if table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'restaurant_profiles') THEN
        UPDATE restaurant_profiles
        SET followers_count = (
            SELECT COUNT(*)::INT FROM followed_restaurants WHERE tenant_id = p_tenant_id
        )
        WHERE tenant_id = p_tenant_id;
    END IF;

    RETURN v_follow_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to unfollow a restaurant (safe, idempotent)
CREATE OR REPLACE FUNCTION unfollow_restaurant(p_tenant_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    DELETE FROM followed_restaurants
    WHERE user_id = auth.uid() AND tenant_id = p_tenant_id;

    -- Update followers_count in restaurant_profiles if table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'restaurant_profiles') THEN
        UPDATE restaurant_profiles
        SET followers_count = GREATEST(0, (
            SELECT COUNT(*)::INT FROM followed_restaurants WHERE tenant_id = p_tenant_id
        ))
        WHERE tenant_id = p_tenant_id;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if user follows a restaurant
CREATE OR REPLACE FUNCTION is_following_restaurant(p_tenant_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM followed_restaurants
        WHERE user_id = auth.uid() AND tenant_id = p_tenant_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get user's followed restaurants with full restaurant info
CREATE OR REPLACE FUNCTION get_followed_restaurants()
RETURNS TABLE (
    follow_id UUID,
    tenant_id UUID,
    restaurant_name TEXT,
    avatar_url TEXT,
    cover_url TEXT,
    address TEXT,
    description TEXT,
    followers_count INT,
    total_posts INT,
    created_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        fr.id AS follow_id,
        fr.tenant_id,
        COALESCE(rp.restaurant_name, t.name) AS restaurant_name,
        COALESCE(rp.avatar_url, t.avatar_url) AS avatar_url,
        COALESCE(rp.cover_url, t.cover_url) AS cover_url,
        COALESCE(rp.address, t.address) AS address,
        COALESCE(rp.description, '') AS description,
        COALESCE(rp.followers_count, 0) AS followers_count,
        COALESCE(rp.total_posts, 0) AS total_posts,
        fr.created_at
    FROM followed_restaurants fr
    INNER JOIN tenants t ON t.id = fr.tenant_id
    LEFT JOIN restaurant_profiles rp ON rp.tenant_id = fr.tenant_id
    WHERE fr.user_id = auth.uid()
    ORDER BY fr.created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
