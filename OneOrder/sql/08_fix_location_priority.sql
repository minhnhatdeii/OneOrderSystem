-- ============================================================
-- FILE: 08_fix_location_priority.sql
-- MỤC ĐÍCH: Fix thuật toán để ưu tiên MẠNH nhà hàng gần.
--           Nhà hàng xa >50km bị nhân phạt 0 → không bao giờ
--           tự nhiên nổi lên top (Android client sẽ xen kẽ sau).
-- CHẠY SAU: 06_food_feed_mock_data.sql
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. Viết lại get_food_recommendations với location ưu tiên MẠNH
-- ─────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION get_food_recommendations(
    p_user_id UUID,
    p_user_lat FLOAT,
    p_user_lng FLOAT,
    p_limit    INTEGER DEFAULT 50
)
RETURNS TABLE (
    post_id  UUID,
    score    FLOAT,
    dist_km  FLOAT   -- trả kèm để Android dùng cho xen-kẽ logic
) AS $$
BEGIN
    RETURN QUERY
    WITH UserInteractions AS (
        SELECT unnest(fp.category_tags) AS tag, COUNT(*) AS tag_weight
        FROM food_posts fp
        LEFT JOIN food_post_likes fpl ON fpl.post_id = fp.id
        LEFT JOIN food_post_views fpv ON fpv.post_id = fp.id
                                      AND fpv.duration_seconds > 5
        WHERE fpl.user_id = p_user_id
           OR fpv.user_id = p_user_id
        GROUP BY tag
    ),
    ScoredPosts AS (
        SELECT
            fp.id,

            -- ── Tọa độ nhà hàng (NULL-safe) ─────────────────────────
            COALESCE(t.latitude,  0.0) AS r_lat,
            COALESCE(t.longitude, 0.0) AS r_lng,

            -- ── Khoảng cách thực (km) ────────────────────────────────
            CASE
                WHEN p_user_lat = 0 AND p_user_lng = 0 THEN 0.0
                ELSE calculate_distance_km(
                        p_user_lat, p_user_lng,
                        COALESCE(t.latitude,  0.0),
                        COALESCE(t.longitude, 0.0))
            END AS dist_km,

            -- ── Content Score (0.40) ─────────────────────────────────
            -- Giảm nhẹ content để nhường chỗ cho location weight cao hơn
            COALESCE(
                (
                    SELECT COALESCE(SUM(ui.tag_weight), 0)
                         / NULLIF((SELECT MAX(tag_weight) FROM UserInteractions), 0)
                    FROM UserInteractions ui
                    WHERE ui.tag = ANY(fp.category_tags)
                ) * 0.40,
                0.0
            ) AS content_score,

            -- ── Freshness (0.05) ────────────────────────────────────
            GREATEST(
                1 - EXTRACT(EPOCH FROM (NOW() - fp.created_at)) / 2592000.0,
                0
            ) * 0.05 AS freshness_score

        FROM food_posts fp
        LEFT JOIN tenants t ON fp.tenant_id = t.id
    ),
    WithLocationScore AS (
        SELECT
            id,
            dist_km,
            content_score,
            freshness_score,

            -- ── Location Score (0.55) với Exponential Decay ──────────
            --
            -- Công thức:  score = 0.55 × e^( -dist / λ )
            --   λ = 3.0  → tại 3 km còn ~37% score
            --              tại 10 km còn ~4%
            --              tại 30 km còn ~0.003%
            --              tại 50+ km ≈ 0 (hard cutoff bên dưới)
            --
            -- Nếu không có GPS (dist=0 do p_user_lat=0), set 0
            CASE
                WHEN p_user_lat = 0 AND p_user_lng = 0
                    THEN 0.0
                WHEN dist_km > 50.0
                    THEN 0.0   -- hard cutoff: >50 km → location_score = 0
                ELSE
                    EXP(-dist_km / 3.0) * 0.55
            END AS location_score

        FROM ScoredPosts
    )
    SELECT
        id                                                   AS post_id,
        content_score + location_score + freshness_score    AS score,
        dist_km
    FROM WithLocationScore
    ORDER BY score DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;


-- ─────────────────────────────────────────────────────────────
-- 2. Cập nhật get_recommended_feed_posts để truyền dist_km ra
--    cho Android client (để client thực hiện xen-kẽ distant posts)
-- ─────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION get_recommended_feed_posts(
    p_user_id  UUID    DEFAULT NULL,
    p_user_lat FLOAT   DEFAULT 0.0,
    p_user_lng FLOAT   DEFAULT 0.0,
    p_limit    INTEGER DEFAULT 20   -- tăng lên 20 để Android có dữ liệu đủ xen kẽ
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
