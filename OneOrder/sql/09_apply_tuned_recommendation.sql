-- ============================================================
-- FILE: 09_apply_tuned_recommendation.sql
-- MỤC ĐÍCH: Sửa lỗi hiển thị món ăn ở quá xa (1139km) lên đầu bảng feed.
--           Áp dụng chính xác trọng số từ TUNING_REPORT.md.resolved.
-- ============================================================

-- DROP các hàm cũ trước khi tạo lại để tránh lỗi "cannot change return type" do thay đổi schema trả về
DROP FUNCTION IF EXISTS get_recommended_feed_posts(UUID, FLOAT, FLOAT, INTEGER);
DROP FUNCTION IF EXISTS get_food_recommendations(UUID, FLOAT, FLOAT, INTEGER);

CREATE OR REPLACE FUNCTION get_food_recommendations(
    p_user_id UUID,
    p_user_lat FLOAT,
    p_user_lng FLOAT,
    p_limit    INTEGER DEFAULT 50
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
    ScoredPosts AS (
        SELECT
            fp.id,

            -- Tính khoảng cách (km)
            CASE
                WHEN p_user_lat = 0 AND p_user_lng = 0 THEN 0.0
                ELSE calculate_distance_km(
                        p_user_lat, p_user_lng,
                        COALESCE(t.latitude,  0.0),
                        COALESCE(t.longitude, 0.0))
            END AS c_dist,

            -- Content Score (0.55) - Tỉ lệ match tag
            COALESCE(
                (
                    SELECT COALESCE(SUM(ui.tag_weight), 0)
                         / NULLIF((SELECT MAX(tag_weight) FROM UserInteractions), 0)
                    FROM UserInteractions ui
                    WHERE ui.tag = ANY(fp.category_tags)
                ) * 0.55,
                0.0
            ) AS content_score,

            -- Freshness (0.10) - Độ mới 1 tháng
            GREATEST(
                1 - EXTRACT(EPOCH FROM (NOW() - fp.created_at)) / 2592000.0,
                0
            ) * 0.10 AS freshness_score

        FROM food_posts fp
        LEFT JOIN tenants t ON fp.tenant_id = t.id
    ),
    WithLocationScore AS (
        SELECT
            id,
            c_dist,
            content_score,
            freshness_score,

            -- Location Score (0.35) với Linear Decay tại mốc 5km
            CASE
                WHEN p_user_lat = 0 AND p_user_lng = 0
                    THEN 0.0
                ELSE
                    GREATEST(1.0 - (c_dist / 5.0), 0.0) * 0.35
            END AS location_score

        FROM ScoredPosts
    )
    SELECT
        id AS post_id,
        
        -- SỬA LỖI: Nếu khoảng cách > 50km, phạt TỔNG ĐIỂM (nhân với 0.0001) 
        -- để đảm bảo luôn chìm dưới đáy feed, nhưng vẫn trả về cho Client
        (content_score + location_score + freshness_score) * 
        CASE 
            WHEN p_user_lat != 0 AND p_user_lng != 0 AND c_dist > 50.0 THEN 0.0001 
            ELSE 1.0 
        END AS score,
        
        c_dist AS dist_km
    FROM WithLocationScore
    ORDER BY score DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- PHẦN 2: Cập nhật get_recommended_feed_posts để trả về khoảng cách
-- ============================================================
CREATE OR REPLACE FUNCTION get_recommended_feed_posts(
    p_user_id  UUID    DEFAULT NULL,
    p_user_lat FLOAT   DEFAULT 0.0,
    p_user_lng FLOAT   DEFAULT 0.0,
    p_limit    INTEGER DEFAULT 20
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
    "isLiked"        BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        fp.id,
        fp.tenant_id,
        COALESCE(t.restaurant_name, 'Nhà hàng')   AS restaurant_name,
        COALESCE(t.address, '')                    AS restaurant_address,
        COALESCE(t.latitude,  0.0)                 AS "restaurantLat",
        COALESCE(t.longitude, 0.0)                 AS "restaurantLng",
        rec.dist_km                                AS "distanceKm",
        t.avatar_url                               AS "restaurantAvatar",
        COALESCE(mi.name, 'Món ăn')               AS "menuItemName",
        fp.images,
        fp.caption,
        COALESCE(mi.price, 0.0)::DOUBLE PRECISION AS price,
        fp.like_count,
        fp.comment_count,
        fp.share_count,
        (
            p_user_id IS NOT NULL AND EXISTS (
                SELECT 1 FROM food_post_likes fpl
                WHERE fpl.post_id = fp.id
                  AND fpl.user_id = p_user_id
            )
        )                                          AS "isLiked"
    FROM get_food_recommendations(p_user_id, p_user_lat, p_user_lng, p_limit) rec
    JOIN food_posts fp   ON fp.id = rec.post_id
    LEFT JOIN tenants  t ON fp.tenant_id = t.id
    LEFT JOIN menu_items mi ON fp.menu_item_id = mi.id
    ORDER BY rec.score DESC;
END;
$$ LANGUAGE plpgsql;
