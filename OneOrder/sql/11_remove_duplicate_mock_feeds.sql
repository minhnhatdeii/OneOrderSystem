-- ============================================================
-- FILE: 11_remove_duplicate_mock_feeds.sql
-- MỤC ĐÍCH: Xóa các bài đăng food feed bị lặp lại nhiều lần 
-- do việc chạy kịch bản tạo dữ liệu mẫu (mock data) nhiều lần.
-- ============================================================

DO $$
BEGIN
    -- Delete all duplicated food_posts based on caption, keeping only the oldest one
    DELETE FROM food_posts
    WHERE id NOT IN (
        SELECT (array_agg(id ORDER BY created_at ASC))[1]
        FROM food_posts
        GROUP BY caption
    );

    RAISE NOTICE 'Đã xóa các feed trùng lặp thành công!';
END $$;
