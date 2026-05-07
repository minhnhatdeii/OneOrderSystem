-- ============================================================
-- FILE: 14_seed_restaurant_profiles.sql
-- MỤC ĐÍCH: Tạo bảng restaurant_profiles từ dữ liệu tenants có sẵn
--            và liên kết với food_posts để hiển thị trong Food Feed
-- ============================================================

-- 1. Tạo bảng restaurant_profiles
CREATE TABLE IF NOT EXISTS restaurant_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    restaurant_name VARCHAR(255) NOT NULL,
    address VARCHAR(500) DEFAULT 'Chưa cập nhật',
    phone VARCHAR(20) DEFAULT 'Chưa cập nhật',
    description TEXT DEFAULT 'Chưa cập nhật',
    avatar_url TEXT,
    cover_url TEXT,
    followers_count INTEGER DEFAULT 0,
    likes_count INTEGER DEFAULT 0,
    total_posts INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Enable RLS
ALTER TABLE restaurant_profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public can view restaurant profiles" ON restaurant_profiles;
CREATE POLICY "Public can view restaurant profiles" ON restaurant_profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated can manage restaurant profiles" ON restaurant_profiles;
CREATE POLICY "Authenticated can manage restaurant profiles" ON restaurant_profiles 
    FOR ALL USING (auth.role() = 'authenticated');

-- 3. Xóa dữ liệu cũ
TRUNCATE restaurant_profiles;

-- 4. Seed dữ liệu từ bảng tenants + food_posts
DO $$
DECLARE
    v_tenant_id UUID;
    v_restaurant_name VARCHAR(255);
    v_address VARCHAR(500);
    v_followers INTEGER;
    v_likes INTEGER;
    v_total_posts INTEGER;
    rec RECORD;
BEGIN
    -- Lặp qua tất cả các tenant
    FOR rec IN
        SELECT 
            t.id as tenant_id,
            COALESCE(t.restaurant_name, 'Nhà hàng ' || t.id::TEXT) as restaurant_name,
            COALESCE(t.address, 'Chưa cập nhật') as address
        FROM tenants t
    LOOP
        v_tenant_id := rec.tenant_id;
        v_restaurant_name := rec.restaurant_name;
        v_address := rec.address;
        
        -- Đếm số bài đăng của nhà hàng này
        SELECT COUNT(*) INTO v_total_posts
        FROM food_posts
        WHERE tenant_id = v_tenant_id;
        
        -- Tính tổng likes từ các bài đăng
        SELECT COALESCE(SUM(like_count), 0)::INTEGER INTO v_likes
        FROM food_posts
        WHERE tenant_id = v_tenant_id;
        
        -- Tạo followers ngẫu nhiên dựa trên likes
        v_followers := GREATEST(10, (v_likes * (0.3 + random() * 0.7))::INTEGER);
        
        -- Nếu không có bài đăng, tạo followers ngẫu nhiên
        IF v_total_posts = 0 THEN
            v_followers := 100 + floor(random() * 5000)::INTEGER;
            v_likes := (v_followers * (2 + random() * 3))::INTEGER;
        END IF;
        
        -- Insert vào restaurant_profiles
        INSERT INTO restaurant_profiles (
            tenant_id, restaurant_name, address,
            followers_count, likes_count, total_posts
        ) VALUES (
            v_tenant_id, v_restaurant_name, v_address,
            v_followers, v_likes, v_total_posts
        )
        ON CONFLICT (tenant_id) DO UPDATE SET
            restaurant_name = EXCLUDED.restaurant_name,
            address = EXCLUDED.address,
            followers_count = EXCLUDED.followers_count,
            likes_count = EXCLUDED.likes_count,
            total_posts = EXCLUDED.total_posts,
            updated_at = NOW();
    END LOOP;
    
    RAISE NOTICE 'Restaurant profiles seeded from tenants!';
END $$;

-- 5. Kiểm tra kết quả
DO $$
DECLARE
    v_count INTEGER;
    v_total_posts_total INTEGER;
BEGIN
    SELECT COUNT(*), COALESCE(SUM(total_posts), 0) INTO v_count, v_total_posts_total 
    FROM restaurant_profiles;
    RAISE NOTICE 'Total restaurant profiles: %', v_count;
    RAISE NOTICE 'Total posts across all restaurants: %', v_total_posts_total;
END $$;

-- 6. Tạo function để lấy profile theo tenant_id
CREATE OR REPLACE FUNCTION get_restaurant_profile_by_tenant(p_tenant_id UUID)
RETURNS SETOF restaurant_profiles AS $$
BEGIN
    RETURN QUERY
    SELECT * FROM restaurant_profiles
    WHERE tenant_id = p_tenant_id
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Tạo view để join restaurant_profiles với food_posts (cho việc hiển thị posts grid)
CREATE OR REPLACE VIEW restaurant_posts_view WITH (security_invoker = on) AS
SELECT 
    rp.id as profile_id,
    rp.tenant_id,
    rp.restaurant_name,
    rp.followers_count,
    rp.likes_count,
    rp.total_posts,
    fp.id as post_id,
    fp.images,
    fp.caption,
    fp.like_count,
    fp.created_at as post_created_at
FROM restaurant_profiles rp
LEFT JOIN food_posts fp ON fp.tenant_id = rp.tenant_id
ORDER BY fp.created_at DESC;
