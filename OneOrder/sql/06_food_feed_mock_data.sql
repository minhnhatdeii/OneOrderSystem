-- 1. Cập nhật bảng Tenants (Thêm các cột nếu chưa có)
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS latitude FLOAT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS longitude FLOAT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- 2. & 3. Đã xóa các hàm gợi ý thức ăn định nghĩa cũ (Xem bản mới nhất tại: 09_apply_tuned_recommendation.sql)

-- 4. INSERT MOCK DATA XỊN ĐỂ THỬ NGHIỆM

-- Nhớ thay 'TENANT_ID_1' bằng ID của 1 nhà hàng thực tế trong bảng tenants của bạn.
-- Ở đây tôi dùng UUID tự gen để mô phỏng. BẠN HÃY TẠO 1 TENANT NẾU CHƯA CÓ HOẶC COPY ID.
-- Trong trường hợp bạn muốn chèn mới (Để phòng ngừa lỗi, hãy đảm bảo cột ID bật Gen Random).

DO $$ 
DECLARE
  v_tenant_1 UUID := gen_random_uuid();
  v_tenant_2 UUID := gen_random_uuid();
  v_menu_1 BIGINT;
  v_menu_2 BIGINT;
  v_menu_3 BIGINT;
  v_owner UUID;
BEGIN
  -- Lấy 1 user ID bất kỳ có sẵn trong hệ thống để làm chủ nhà hàng (tránh lỗi khóa ngoại)
  SELECT id INTO v_owner FROM auth.users LIMIT 1;
  IF v_owner IS NULL THEN
    -- Nếu chưa có ai, chúng ta đành tắt constraint hoặc giả định bạn phải tạo tk trước.
    -- Để an toàn cho script chạy được, ta gen tạm nhưng có thể lỗi FK nếu auth.users trống.
    v_owner := gen_random_uuid();
  END IF;

  -- Tạo 2 Nhà Hàng (Thay t.id vào nếu bạn lấy ID nhà hàng thật của bạn)
  INSERT INTO tenants (id, owner_id, restaurant_name, address, latitude, longitude, avatar_url, created_at, updated_at)
  VALUES 
    (v_tenant_1, v_owner, 'Phở Gia Truyền Bát Đàn', '49 Bát Đàn, Quận Hoàn Kiếm, Hà Nội', 21.0315, 105.8458, 'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3', NOW(), NOW()),
    (v_tenant_2, v_owner, 'Highlands Coffee', '12 Lê Lợi, TP Thanh Hóa', 19.8000, 105.7667, 'https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3', NOW(), NOW());

  -- Tạo 3 menu_items cho 2 nhà hàng
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES 
    (v_tenant_1, NULL, 'Phở Bò Tái Nạm', 'Phở bò gia truyền nước cốt ngọt thanh hầm củ quả', 65000, 'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500', true) RETURNING id INTO v_menu_1;
  
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES 
    (v_tenant_1, NULL, 'Quẩy Nóng', 'Quẩy chiên giòn ăn kèm phở', 10000, 'https://images.unsplash.com/photo-1601000938259-9e92002320b2?w=500', true) RETURNING id INTO v_menu_2;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES 
    (v_tenant_2, NULL, 'Trà Sen Vàng', 'Thức uống Signature Highlands', 55000, 'https://images.unsplash.com/photo-1622485542289-498c8c560241?w=500', true) RETURNING id INTO v_menu_3;

  -- Tạo 3 food_posts ứng với menu_items
  INSERT INTO food_posts (tenant_id, menu_item_id, images, caption, category_tags, like_count, comment_count, share_count, view_count, is_trending)
  VALUES 
    (v_tenant_1, v_menu_1, 
    '[{"url": "https://plus.unsplash.com/premium_photo-1661439265910-4eddb0a1a5be?w=500&auto=format&fit=crop", "layout": "PORTRAIT"}]', 
    'Phở Tái nạm cốt tủy bò siêu chất lượng cho ngày mưa rào Hà Nội. Quán đông nhưng phục vụ rất nhiệt tình!!', 
    ARRAY['pho', 'noodle', 'vietnam_food', 'nóng', 'bò'], 1450, 42, 18, 5032, true),
    
    (v_tenant_1, v_menu_2, 
    '[{"url": "https://images.unsplash.com/photo-1601000938259-9e92002320b2?w=500&auto=format&fit=crop", "layout": "SQUARE"}]', 
    'Chấm quẩy chiên ngập nước lèo ta nói nó phê gì đâu 🤤', 
    ARRAY['an_vat', 'vietnam_food', 'gion', 'pho'], 345, 12, 3, 1024, false),

    (v_tenant_2, v_menu_3, 
    '[{"url": "https://images.unsplash.com/photo-1622485542289-498c8c560241?w=500&auto=format&fit=crop", "layout": "TALL"}, {"url": "https://images.unsplash.com/photo-1549646452-fddba4c207d1?w=500&auto=format&fit=crop", "layout": "PORTRAIT"}]', 
    'Trà sen vàng Highlands luôn là lựa chọn số 1 khi chạy deadline đêm ☕️ Lớp macchiato đỉnh của chóp.', 
    ARRAY['tra', 'ngot', 'highlands', 'coffee_shop', 'do_uong'], 2890, 150, 65, 8740, true);
END $$;
