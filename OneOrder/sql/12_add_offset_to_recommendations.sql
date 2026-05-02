-- ============================================================
-- FILE: 12_add_offset_to_recommendations.sql
-- MỤC ĐÍCH: Thêm tham số offset vào các hàm get_food_recommendations
--           và get_recommended_feed_posts để hỗ trợ Lazy Loading (Pagination)
-- ============================================================

-- DROP các hàm cũ trước khi tạo lại
DROP FUNCTION IF EXISTS get_recommended_feed_posts(UUID, FLOAT, FLOAT, INTEGER);
DROP FUNCTION IF EXISTS get_food_recommendations(UUID, FLOAT, FLOAT, INTEGER);
DROP FUNCTION IF EXISTS get_recommended_feed_posts(UUID, FLOAT, FLOAT, INTEGER, INTEGER);
DROP FUNCTION IF EXISTS get_food_recommendations(UUID, FLOAT, FLOAT, INTEGER, INTEGER);

CREATE OR REPLACE FUNCTION get_food_recommendations(
    p_user_id UUID,
    p_user_lat FLOAT,
    p_user_lng FLOAT,
    p_limit    INTEGER DEFAULT 50,
    p_offset   INTEGER DEFAULT 0
)
RETURNS TABLE (
    post_id  UUID,
    score    FLOAT,
    dist_km  FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH UserInteractions AS (
        SELECT unnest(fp.category_tags) AS tag, COUNT(*) AS tag_weight
        FROM food_posts fp
        LEFT JOIN food_post_likes fpl ON fpl.post_id = fp.id
        LEFT JOIN food_post_views fpv ON fpv.post_id = fp.id AND fpv.duration_seconds > 5
        WHERE fpl.user_id = p_user_id OR fpv.user_id = p_user_id
        GROUP BY tag
    ),
    -- [MỚI] Collaborative Filtering: Tìm 50 người dùng có sở thích giống nhất
    SimilarUsers AS (
        SELECT fpl2.user_id, COUNT(*) AS similarity_score
        FROM food_post_likes fpl1
        JOIN food_post_likes fpl2 ON fpl1.post_id = fpl2.post_id
        WHERE fpl1.user_id = p_user_id AND fpl2.user_id != p_user_id
        GROUP BY fpl2.user_id
        ORDER BY similarity_score DESC
        LIMIT 50
    ),
    -- [MỚI] Collaborative Filtering: Tính điểm cho các bài viết được những người dùng kia thích
    CF_Scores AS (
        SELECT fpl.post_id, SUM(su.similarity_score) AS raw_cf_score
        FROM SimilarUsers su
        JOIN food_post_likes fpl ON su.user_id = fpl.user_id
        GROUP BY fpl.post_id
    ),
    ScoredPosts AS (
        SELECT
            fp.id,

            -- Tính khoảng cách (km) — LUÔN tính để trả về khoảng cách thực cho Client
            -- Ngay cả khi user chưa cấp GPS, vẫn trả về distance từ (0,0) → nhà hàng
            -- (Edge Function sẽ recalculate lại với GPS thật khi user có location)
            calculate_distance_km(
                p_user_lat, p_user_lng,
                COALESCE(t.latitude,  0.0),
                COALESCE(t.longitude, 0.0)) AS c_dist,

            -- Content Score (0.35) - Tỉ lệ match tag
            COALESCE(
                (
                    SELECT COALESCE(SUM(ui.tag_weight), 0)
                         / NULLIF((SELECT MAX(tag_weight) FROM UserInteractions), 0)
                    FROM UserInteractions ui
                    WHERE ui.tag = ANY(fp.category_tags)
                ) * 0.35,
                0.0
            ) AS content_score,

            -- Freshness (0.05) - Độ mới 1 tháng
            GREATEST(
                1 - EXTRACT(EPOCH FROM (NOW() - fp.created_at)) / 2592000.0,
                0
            ) * 0.05 AS freshness_score,

            -- [MỚI] Trending (0.15) - Lượt tương tác
            LEAST(
                LN(1 + 
                   COALESCE(fp.like_count, 0) * 1.0 + 
                   COALESCE(fp.comment_count, 0) * 2.0 + 
                   COALESCE(fp.share_count, 0) * 3.0
                ) / LN(100), 
                1.0
            ) * 0.15 AS trending_score,

            -- [MỚI] Collaborative Filtering (0.15)
            LEAST(COALESCE(cf.raw_cf_score, 0) / 10.0, 1.0) * 0.15 AS cf_score

        FROM food_posts fp
        LEFT JOIN tenants t ON fp.tenant_id = t.id
        LEFT JOIN CF_Scores cf ON cf.post_id = fp.id
    ),
    WithLocationScore AS (
        SELECT
            id,
            c_dist,
            content_score,
            freshness_score,
            trending_score,
            cf_score,

            -- Location Score (0.30) với Linear Decay tại mốc 5km
            CASE
                WHEN p_user_lat = 0 AND p_user_lng = 0
                    THEN 0.0
                ELSE
                    GREATEST(1.0 - (c_dist / 5.0), 0.0) * 0.30
            END AS location_score

        FROM ScoredPosts
    )
    SELECT
        id AS post_id,
        
        -- SỬA LỖI: Nếu khoảng cách > 50km, phạt TỔNG ĐIỂM (nhân với 0.0001) 
        -- để đảm bảo luôn chìm dưới đáy feed, nhưng vẫn trả về cho Client
        (content_score + location_score + freshness_score + trending_score + cf_score) * 
        CASE 
            WHEN p_user_lat != 0 AND p_user_lng != 0 AND c_dist > 50.0 THEN 0.0001 
            ELSE 1.0 
        END AS score,
        
        c_dist AS dist_km
    FROM WithLocationScore
    ORDER BY score DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- PHẦN 2: get_recommended_feed_posts hỗ trợ p_offset
-- ============================================================
CREATE OR REPLACE FUNCTION get_recommended_feed_posts(
    p_user_id  UUID    DEFAULT NULL,
    p_user_lat FLOAT   DEFAULT 0.0,
    p_user_lng FLOAT   DEFAULT 0.0,
    p_limit    INTEGER DEFAULT 20,
    p_offset   INTEGER DEFAULT 0
)
RETURNS TABLE (
    id               UUID,
    tenant_id        UUID,
    restaurant_name  TEXT,
    restaurant_address TEXT,
    "restaurantLat"  FLOAT,
    "restaurantLng"  FLOAT,
    "distanceKm"     FLOAT,
    "restaurantAvatar" TEXT,
    "menuItemName"   TEXT,
    images           JSONB,
    caption          TEXT,
    price            DOUBLE PRECISION,
    like_count       INTEGER,
    comment_count    INTEGER,
    share_count      INTEGER,
    "isLiked"        BOOLEAN,
    tags             TEXT,         -- Full tags string "cuisine:italian|flavor:spicy" for IDF scoring
    created_at       TIMESTAMPTZ,  -- For freshness score
    cuisine_tags     TEXT[],        -- For MMR diversity reranking
    dish_type_tags   TEXT[]         -- For MMR diversity reranking
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        fp.id,
        fp.tenant_id,
        COALESCE(t.restaurant_name, 'Nhà hàng')    AS restaurant_name,
        COALESCE(t.address, '')                    AS restaurant_address,
        COALESCE(t.latitude,  0.0)                 AS "restaurantLat",
        COALESCE(t.longitude, 0.0)                 AS "restaurantLng",
        rec.dist_km                                AS "distanceKm",
        t.avatar_url                               AS "restaurantAvatar",
        COALESCE(mi.name, 'Món ăn')                AS "menuItemName",
        fp.images,
        fp.caption,
        COALESCE(mi.price, 0.0)::DOUBLE PRECISION  AS price,
        fp.like_count,
        fp.comment_count,
        fp.share_count,
        (
            p_user_id IS NOT NULL AND EXISTS (
                SELECT 1 FROM food_post_likes fpl
                WHERE fpl.post_id = fp.id
                  AND fpl.user_id = p_user_id
            )
        )                                          AS "isLiked",
        -- Parse category_tags into individual fields for Edge Function scoring
        -- category_tags is TEXT[] like ARRAY['cuisine:italian', 'flavor:spicy', 'protein:beef']
        -- We convert to TEXT for the full string and extract cuisine/dish_type arrays
        COALESCE(
            (SELECT string_agg(unnest, '|') FROM unnest(fp.category_tags)), ''
        )                                          AS tags,
        fp.created_at,
        -- Extract cuisine tags into separate array
        COALESCE(
            (SELECT array_agg(substring(unnest FROM 9))
             FROM unnest(fp.category_tags) WHERE unnest LIKE 'cuisine:%'),
            ARRAY[]::TEXT[]
        )                                          AS cuisine_tags,
        -- Extract dish_type tags into separate array
        COALESCE(
            (SELECT array_agg(substring(unnest FROM 12))
             FROM unnest(fp.category_tags) WHERE unnest LIKE 'dish_type:%'),
            ARRAY[]::TEXT[]
        )                                          AS dish_type_tags
    FROM get_food_recommendations(p_user_id, p_user_lat, p_user_lng, p_limit, p_offset) rec
    JOIN food_posts fp   ON fp.id = rec.post_id
    LEFT JOIN tenants  t ON fp.tenant_id = t.id
    LEFT JOIN menu_items mi ON fp.menu_item_id = mi.id
    ORDER BY rec.score DESC;  -- Quan trọng để thứ tự được giữ nguyên với lazy loading
END;
$$ LANGUAGE plpgsql;
