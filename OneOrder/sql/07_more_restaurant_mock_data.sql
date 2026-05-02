-- ============================================================
-- FILE: 07_more_restaurant_mock_data.sql
-- MỤC ĐÍCH: Thêm 12 nhà hàng mới với dữ liệu thực tế để thử nghiệm
--           chức năng Food Feed recommendation + GPS location.
-- CHÚ Ý   : Chạy sau khi đã chạy 06_food_feed_mock_data.sql
-- ============================================================

DO $$
DECLARE
  -- ── 12 nhà hàng ────────────────────────────────────────────
  v_t1  UUID := gen_random_uuid();   -- Bún Bò Huế Mụ Rớt
  v_t2  UUID := gen_random_uuid();   -- Bánh Mì Phương
  v_t3  UUID := gen_random_uuid();   -- Cơm Tấm Bà Ghẹ
  v_t4  UUID := gen_random_uuid();   -- Lẩu Thái Koh Yam
  v_t5  UUID := gen_random_uuid();   -- Bít Tết Năm Dưỡng
  v_t6  UUID := gen_random_uuid();   -- Gà Rán BBQ Chicken
  v_t7  UUID := gen_random_uuid();   -- Hủ Tiếu Nam Vang
  v_t8  UUID := gen_random_uuid();   -- Trà Sữa Gong Cha
  v_t9  UUID := gen_random_uuid();   -- Bún Chả Obama
  v_t10 UUID := gen_random_uuid();   -- Pizza 4P's
  v_t11 UUID := gen_random_uuid();   -- Dimsum Đường Phố
  v_t12 UUID := gen_random_uuid();   -- Bánh Xèo Mười Xiềm

  -- ── Menu items ─────────────────────────────────────────────
  m1a BIGINT; m1b BIGINT;
  m2a BIGINT; m2b BIGINT;
  m3a BIGINT; m3b BIGINT;
  m4a BIGINT; m4b BIGINT;
  m5a BIGINT; m5b BIGINT;
  m6a BIGINT; m6b BIGINT;
  m7a BIGINT; m7b BIGINT;
  m8a BIGINT; m8b BIGINT;
  m9a BIGINT; m9b BIGINT;
  m10a BIGINT; m10b BIGINT;
  m11a BIGINT; m11b BIGINT;
  m12a BIGINT; m12b BIGINT;

  v_owner UUID;
BEGIN
  -- Lấy user có sẵn làm owner
  SELECT id INTO v_owner FROM auth.users LIMIT 1;
  IF v_owner IS NULL THEN
    v_owner := gen_random_uuid();
  END IF;

  -- ══════════════════════════════════════════════════════════
  --  INSERT 12 TENANTS (Nhà hàng)
  --  Tọa độ trải đều ở Hà Nội & TP.HCM để test GPS radius
  -- ══════════════════════════════════════════════════════════
  INSERT INTO tenants (id, owner_id, restaurant_name, address, latitude, longitude, avatar_url, created_at, updated_at) VALUES

    -- ── Hà Nội (khu vực Hoàn Kiếm / Ba Đình / Đống Đa) ──────
    (v_t1,  v_owner, 'Bún Bò Huế Mụ Rớt',
     '18 Phan Chu Trinh, Hoàn Kiếm, Hà Nội',
     21.0285, 105.8542,
     'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t2,  v_owner, 'Bánh Mì Phương Hội An',
     '2B Phan Châu Trinh, Hội An (Chi Nhánh HN: 12 Hàng Bông)',
     21.0336, 105.8509,
     'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t9,  v_owner, 'Bún Chả Hương Liên (Obama)',
     '24 Lê Văn Hưu, Hai Bà Trưng, Hà Nội',
     21.0187, 105.8504,
     'https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t11, v_owner, 'Dimsum Đường Phố Hà Thành',
     '36 Cầu Gỗ, Hoàn Kiếm, Hà Nội',
     21.0330, 105.8534,
     'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t8,  v_owner, 'Gong Cha Hà Nội',
     '55 Tràng Tiền, Hoàn Kiếm, Hà Nội',
     21.0248, 105.8568,
     'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    -- ── TP. Hồ Chí Minh (Quận 1 / 3 / Bình Thạnh) ──────────
    (v_t3,  v_owner, 'Cơm Tấm Bà Ghẹ',
     '84/1 Đinh Tiên Hoàng, Bình Thạnh, TP.HCM',
     10.8023, 106.7150,
     'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t4,  v_owner, 'Lẩu Thái Koh Yam',
     '205 Nguyễn Xí, Bình Thạnh, TP.HCM',
     10.8105, 106.7082,
     'https://images.unsplash.com/photo-1534482421-64566f976cfa?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t5,  v_owner, 'Bít Tết Năm Dưỡng',
     '197 Phan Xích Long, Phú Nhuận, TP.HCM',
     10.8010, 106.6849,
     'https://images.unsplash.com/photo-1558030006-450675393462?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t6,  v_owner, 'BBQ Chicken Sài Gòn',
     '117 Lý Tự Trọng, Quận 1, TP.HCM',
     10.7736, 106.6985,
     'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t7,  v_owner, 'Hủ Tiếu Nam Vang Tân Kỳ',
     '234 Đinh Tiên Hoàng, Bình Thạnh, TP.HCM',
     10.8041, 106.7162,
     'https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t10, v_owner, 'Pizza 4P''s Hai Bà Trưng',
     '8 Thái Văn Lung, Bến Nghé, Quận 1, TP.HCM',
     10.7801, 106.7038,
     'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW()),

    (v_t12, v_owner, 'Bánh Xèo Mười Xiềm',
     '204 Nguyễn Trãi, Quận 1, TP.HCM',
     10.7671, 106.6908,
     'https://images.unsplash.com/photo-1625400851892-8e1b4c9bdfbf?w=500&auto=format&fit=crop&q=60',
     NOW(), NOW());

  -- ══════════════════════════════════════════════════════════
  --  INSERT MENU ITEMS (2 món / nhà hàng)
  -- ══════════════════════════════════════════════════════════

  -- T1: Bún Bò Huế Mụ Rớt
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t1, NULL, 'Bún Bò Huế Đặc Biệt',
          'Nước dùng hầm xương, thịt bò mềm, chả cua đặc biệt',
          75000, 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500', true)
  RETURNING id INTO m1a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t1, NULL, 'Bún Bò Giò Heo',
          'Thêm giò heo ninh mềm, ăn kèm rau sống chua',
          85000, 'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500', true)
  RETURNING id INTO m1b;

  -- T2: Bánh Mì Phương
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t2, NULL, 'Bánh Mì Thịt Đặc Biệt',
          'Bánh giòn ổ nhỏ, pate, thủ, jambon, dưa leo, rau thơm',
          35000, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=500', true)
  RETURNING id INTO m2a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t2, NULL, 'Bánh Mì Trứng Ốp La',
          'Trứng gà ốp la, xốt tỏi ớt, ăn sáng ngon',
          25000, 'https://images.unsplash.com/photo-1525351484163-7529414344d8?w=500', true)
  RETURNING id INTO m2b;

  -- T3: Cơm Tấm Bà Ghẹ
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t3, NULL, 'Cơm Tấm Sườn Bì Chả',
          'Sườn nướng than, bì, chả trứng, đồ chua, nước mắm pha',
          70000, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500', true)
  RETURNING id INTO m3a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t3, NULL, 'Cơm Tấm Gà Nướng',
          'Đùi gà nướng mật ong, cơm tấm dẻo, ăn kèm canh bắp',
          65000, 'https://images.unsplash.com/photo-1598514983318-2f64f8f4796c?w=500', true)
  RETURNING id INTO m3b;

  -- T4: Lẩu Thái Koh Yam
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t4, NULL, 'Lẩu Thái Hải Sản',
          'Tôm, mực, cá, nấm, chua cay chuẩn vị Thái Lan',
          280000, 'https://images.unsplash.com/photo-1534482421-64566f976cfa?w=500', true)
  RETURNING id INTO m4a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t4, NULL, 'Tom Kha Gai',
          'Súp gà nấu coconut milk, sả, riềng, lá chanh',
          120000, 'https://images.unsplash.com/photo-1547592180-85f173990554?w=500', true)
  RETURNING id INTO m4b;

  -- T5: Bít Tết Năm Dưỡng
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t5, NULL, 'Bít Tết Bò Úc',
          'Thịt bò Úc hạng nhất, áp chảo butter rosemary, kèm khoai tây chiên',
          220000, 'https://images.unsplash.com/photo-1558030006-450675393462?w=500', true)
  RETURNING id INTO m5a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t5, NULL, 'Sườn Heo Nướng BBQ',
          'Sườn heo ngâm sốt BBQ mật ong, nướng than hoa',
          180000, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500', true)
  RETURNING id INTO m5b;

  -- T6: BBQ Chicken
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t6, NULL, 'Gà Rán Half Half',
          'Nửa gà rán giòn + nửa gà nướng sốt Hàn Quốc',
          199000, 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=500', true)
  RETURNING id INTO m6a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t6, NULL, 'Combo Gà Rán + Khoai + Nước',
          '2 miếng gà, khoai tây chiên lớn, nước ngọt tự chọn',
          145000, 'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=500', true)
  RETURNING id INTO m6b;

  -- T7: Hủ Tiếu Nam Vang
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t7, NULL, 'Hủ Tiếu Khô Tôm Thịt',
          'Hủ tiếu dai, tôm tươi lớn, thịt nạc xay, hành phi thơm',
          65000, 'https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=500', true)
  RETURNING id INTO m7a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t7, NULL, 'Hủ Tiếu Nước Đặc Biệt',
          'Nước dùng trong vắt hầm từ xương heo và tôm khô',
          70000, 'https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500', true)
  RETURNING id INTO m7b;

  -- T8: Gong Cha
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t8, NULL, 'Trà Sữa Trân Châu Đường Đen',
          'Tiger Sugar style, trân châu đường đen dẻo dai, sữa tươi thêm kem cheese',
          65000, 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500', true)
  RETURNING id INTO m8a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t8, NULL, 'Matcha Latte Kem Mặn',
          'Matcha Nhật Bản nguyên chất, thêm lớp kem mặn béo ngậy',
          70000, 'https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=500', true)
  RETURNING id INTO m8b;

  -- T9: Bún Chả Obama
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t9, NULL, 'Bún Chả Nướng Than Hoa',
          'Chả viên + thịt ba chỉ nướng than hoa, bún bún, nước chấm chua ngọt',
          65000, 'https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=500', true)
  RETURNING id INTO m9a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t9, NULL, 'Nem Rán Giòn',
          'Nem rán vàng giòn, nhân thịt + miến + mộc nhĩ',
          40000, 'https://images.unsplash.com/photo-1614251055880-ee96e4803393?w=500', true)
  RETURNING id INTO m9b;

  -- T10: Pizza 4P's
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t10, NULL, 'Pizza Burrata Cà Chua Bi',
          'Phô mai burrata Ý tươi, cà chua bi, lá húng quế, dầu olive',
          320000, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=500', true)
  RETURNING id INTO m10a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t10, NULL, 'Pizza Wagyu Beef',
          'Thịt bò Wagyu A5, nấm truffle, phô mai 5 loại, áp chảo',
          480000, 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500', true)
  RETURNING id INTO m10b;

  -- T11: Dimsum Đường Phố
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t11, NULL, 'Há Cảo Tôm Hấp',
          '5 cái há cảo tôm tươi hấp, chấm dầu hào + ớt tươi',
          55000, 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500', true)
  RETURNING id INTO m11a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t11, NULL, 'Sủi Cảo Tôm Thịt Soup',
          'Hoành thánh tôm thịt trong nước dùng gà hầm xương, hành lá',
          65000, 'https://images.unsplash.com/photo-1647674879705-1a543f9d8f4a?w=500', true)
  RETURNING id INTO m11b;

  -- T12: Bánh Xèo Mười Xiềm
  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t12, NULL, 'Bánh Xèo Tôm Thịt Giòn',
          'Bánh xèo giòn rụm, tôm tươi, thịt ba chỉ, giá đỗ, ăn kèm rau sống và nước mắm',
          75000, 'https://images.unsplash.com/photo-1625400851892-8e1b4c9bdfbf?w=500', true)
  RETURNING id INTO m12a;

  INSERT INTO menu_items (tenant_id, category_id, name, description, price, image_url, is_available)
  VALUES (v_t12, NULL, 'Bánh Khọt Coconut Shrimp',
          'Bánh khọt nước cốt dừa, tôm tươi to, ăn kèm rau sống Miền Nam',
          65000, 'https://images.unsplash.com/photo-1617622141573-c6ea7c06ac50?w=500', true)
  RETURNING id INTO m12b;

  -- ══════════════════════════════════════════════════════════
  --  INSERT FOOD POSTS (2 post mỗi nhà hàng = 24 posts)
  -- ══════════════════════════════════════════════════════════
  INSERT INTO food_posts (tenant_id, menu_item_id, images, caption, category_tags,
                          like_count, comment_count, share_count, view_count, is_trending)
  VALUES

  -- ── Bún Bò Huế Mụ Rớt ────────────────────────────────────
  (v_t1, m1a,
   '[{"url":"https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Bún Bò Huế chuẩn công thức Cố Đô 30 năm. Nước lèo cay thơm hầm 8 tiếng, thịt bò mềm tan. Trời lạnh Hà Nội mà ăn bát này thì thôi rồi 😍🌶️',
   ARRAY['bun_bo_hue','mien_trung','cay','nóng','noodle','soup'],
   3420, 87, 34, 12800, true),

  (v_t1, m1b,
   '[{"url":"https://images.unsplash.com/photo-1547592166-23ac45744acd?w=800&auto=format&fit=crop","layout":"PORTRAIT"},{"url":"https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Thêm cái giò heo vào thì nó thành HOÀN HẢO 🤤 Giò ninh 4 tiếng, mềm rục, hút tuỷ là biết ngay trình của bếp.',
   ARRAY['bun_bo_hue','gioT_heo','mien_trung','nóng','phong_phu'],
   2100, 55, 18, 7300, false),

  -- ── Bánh Mì Phương ───────────────────────────────────────
  (v_t2, m2a,
   '[{"url":"https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&auto=format&fit=crop","layout":"LANDSCAPE"}]',
   'Bánh mì Phương danh bất hư truyền! Ổ bánh ngắn thôi nhưng nhân đầy ú ụ. Pate, thủ, jambon, dưa leo giòn rụm — 35k mà đi khắp thế giới rồi 🌍🥖',
   ARRAY['banh_mi','an_sang','viet_food','street_food','gion'],
   5890, 220, 98, 23400, true),

  (v_t2, m2b,
   '[{"url":"https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Combo sáng cực ngon: bánh mì trứng ốp la + cà phê sữa đá. Giản dị nhưng ngon hơn nhiều thứ phức tạp 😄☕',
   ARRAY['banh_mi','trung','an_sang','street_food','gion'],
   1200, 30, 10, 4500, false),

  -- ── Cơm Tấm Bà Ghẹ ──────────────────────────────────────
  (v_t3, m3a,
   '[{"url":"https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=800&auto=format&fit=crop","layout":"PORTRAIT"},{"url":"https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Cơm tấm Sài Gòn chuẩn nhất phải là đây! Sườn nướng than đỏ au, bì giòn, chả trứng mịn. Nước mắm pha vừa đủ ngọt mặn 🔥🍚',
   ARRAY['com_tam','suon_nuong','sai_gon','mien_nam','rice','viet_food'],
   4560, 133, 62, 18200, true),

  (v_t3, m3b,
   '[{"url":"https://images.unsplash.com/photo-1598514983318-2f64f8f4796c?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Option healthy hơn: cơm tấm GÀ NƯỚNG 🍗 Đùi gà mật ong nướng vàng, thơm lừng. Với cơm tấm dẻo hạt nhỏ thì khó cưỡng lắm!',
   ARRAY['com_tam','ga_nuong','sai_gon','mien_nam','chicken'],
   2870, 76, 28, 9800, false),

  -- ── Lẩu Thái Koh Yam ─────────────────────────────────────
  (v_t4, m4a,
   '[{"url":"https://images.unsplash.com/photo-1534482421-64566f976cfa?w=800&auto=format&fit=crop","layout":"LANDSCAPE"},{"url":"https://images.unsplash.com/photo-1547592180-85f173990554?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Lẩu Thái chuẩn vị Koh Samui! Nước dùng Tom Yum chua cay ngập mề, hải sản tươi roi rói. Mùa mưa mà ăn lẩu thì ĐỈNH của CHÓP 🦐🌶️🔥',
   ARRAY['lau_thai','hai_san','cay','tom_yum','hotpot','seafood'],
   6720, 209, 87, 31500, true),

  (v_t4, m4b,
   '[{"url":"https://images.unsplash.com/photo-1547592180-85f173990554?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Tom Kha Gai — súp gà coconut chuẩn Thái. Béo thơm lá chanh Kaffir, sả, riềng. Một bát là muốn order thêm ngay 🥥🍋',
   ARRAY['tom_kha','thai_food','soup','coconut','ga','healthy'],
   1980, 54, 22, 7200, false),

  -- ── Bít Tết Năm Dưỡng ───────────────────────────────────
  (v_t5, m5a,
   '[{"url":"https://images.unsplash.com/photo-1558030006-450675393462?w=800&auto=format&fit=crop","layout":"LANDSCAPE"}]',
   'Bít tết bò Úc áp chảo butter 🧈🥩 Lớp vỏ ngoài cháy thơm, bên trong hồng hào medium rare đúng chuẩn. Khoai tây chiên mỡ bò — không thể chê.',
   ARRAY['bit_tet','bo_uc','steak','western','fine_dining','butter'],
   3890, 102, 45, 14300, true),

  (v_t5, m5b,
   '[{"url":"https://images.unsplash.com/photo-1544025162-d76694265947?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Sườn Heo BBQ 🍖 Ướp qua đêm, nướng than hoa tỉ mỉ. Sốt BBQ tự làm — mật ong, cà chua, tỏi. Chấm xong muốn liếm cả ngón tay!',
   ARRAY['suon_heo','bbq','nuong','sai_gon','pork','smoky'],
   2340, 68, 29, 8900, false),

  -- ── BBQ Chicken Sài Gòn ──────────────────────────────────
  (v_t6, m6a,
   '[{"url":"https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=800&auto=format&fit=crop","layout":"SQUARE"},{"url":"https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Half & Half = best of both worlds! Nửa gà rán giòn kiểu Hàn, nửa nướng sốt yangnyeom cay ngọt. Khi nào không biết chọn cái gì thì chọn cái này 😂🍗',
   ARRAY['ga_ran','han_quoc','korean_food','bbq','fried_chicken','combo'],
   7820, 312, 143, 42600, true),

  (v_t6, m6b,
   '[{"url":"https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=800&auto=format&fit=crop","layout":"LANDSCAPE"}]',
   'Combo gà rán giá sinh viên mà ngon như nhà hàng cao cấp 💪 Gà giòn, khoai tây mập, nước lạnh. Trưa nào cũng có thể ăn!',
   ARRAY['ga_ran','combo','gia_re','khoai_tay','fast_food'],
   3120, 89, 37, 11900, false),

  -- ── Hủ Tiếu Nam Vang ─────────────────────────────────────
  (v_t7, m7a,
   '[{"url":"https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Hủ Tiếu Nam Vang khô — Phiên bản KHÔNG nước lèo mà vẫn thần thánh 🤯 Trộn đều với sốt hành phi, mỡ hành thơm, rồi húp nước lèo riêng. Kiểu ăn này mới là đỉnh!',
   ARRAY['hu_tieu','kho','mien_nam','noodle','sai_gon','street_food'],
   2980, 71, 24, 10300, true),

  (v_t7, m7b,
   '[{"url":"https://images.unsplash.com/photo-1585032226651-759b368d7246?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Hủ tiếu nước trong vắt nhìn là biết tay nghề cao 😍 Tôm khô + xương heo hầm 6 tiếng, ngọt thanh tự nhiên. Không bột ngọt nhưng ngọt hơn bao nhiêu chỗ.',
   ARRAY['hu_tieu','nuoc','mien_nam','noodle','soup','clean'],
   1760, 42, 15, 6200, false),

  -- ── Gong Cha Hà Nội ──────────────────────────────────────
  (v_t8, m8a,
   '[{"url":"https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=800&auto=format&fit=crop","layout":"PORTRAIT"},{"url":"https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Trà sữa đường đen mà nhìn là sướng mắt 🖤🧋 Hủ trân châu tự làm, dẻo dai, đường đen tan ra ngọt lịm pha giòn sữa tươi. Kem cheese 50% béo ngậy không thể cưỡng!',
   ARRAY['tra_sua','tran_chau','duong_den','kem_cheese','do_uong','instagram'],
   8940, 405, 198, 56200, true),

  (v_t8, m8b,
   '[{"url":"https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Matcha Latte kem mặn mà uống một lần là ghiền 💚 Matcha Nhật nguyên chất đắng nhẹ, gặp kem mặn béo = combo hoàn hảo cho buổi chiều.',
   ARRAY['matcha','latte','kem_man','do_uong','nhat_ban','aesthetic'],
   4320, 118, 59, 19800, false),

  -- ── Bún Chả Obama ─────────────────────────────────────────
  (v_t9, m9a,
   '[{"url":"https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Quán bún chả Obama từng ăn! Chả nướng than hoa thơm nức mũi, bún tươi trắng, rau sống sạch, nước chấm ngọt chua hoàn hảo. Hà Nội mà không ăn đây là phí chuyến đi! 🇻🇳',
   ARRAY['bun_cha','obama','ha_noi','tin_tuc','noodle','grill','bbq'],
   12400, 567, 238, 78900, true),

  (v_t9, m9b,
   '[{"url":"https://images.unsplash.com/photo-1614251055880-ee96e4803393?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Nem rán kèm bún chả mới là cặp đôi hoàn hảo! Nem giòn rụm, nhân thịt miến đầy đặn. Chấm mắm chua ngọt + ớt tươi thái nhỏ 🥢✨',
   ARRAY['nem_ran','chien','viet_food','ha_noi','gion','an_vat'],
   2870, 72, 31, 10100, false),

  -- ── Pizza 4P's ────────────────────────────────────────────
  (v_t10, m10a,
   '[{"url":"https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800&auto=format&fit=crop","layout":"LANDSCAPE"},{"url":"https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&auto=format&fit=crop","layout":"SQUARE"}]',
   'Pizza 4P''s Burrata — Luxury pizza made in Saigon 🍕✨ Phô mai burrata chảy ra khi cắt đôi, cà chua bi chua ngọt, từng chiếc lá húng quế tươi. Không cần đến Ý nữa!',
   ARRAY['pizza','burrata','italian','sai_gon','fine_dining','cheese'],
   9870, 345, 167, 61200, true),

  (v_t10, m10b,
   '[{"url":"https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Wagyu Beef Pizza — khi pizza gặp thịt bò Wagyu A5 🥩🍕 Giá không rẻ nhưng cắn một miếng là hiểu tại sao. Nấm truffle thơm không thể tả, phô mai kéo dài...',
   ARRAY['pizza','wagyu','truffle','premium','cheese','sai_gon'],
   6540, 198, 89, 38700, true),

  -- ── Dimsum Đường Phố Hà Thành ────────────────────────────
  (v_t11, m11a,
   '[{"url":"https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&auto=format&fit=crop","layout":"SQUARE"},{"url":"https://images.unsplash.com/photo-1647674879705-1a543f9d8f4a?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Há cảo tôm hấp chuẩn Hong Kong ngay giữa phố cổ Hà Nội! 🦐 Vỏ bánh trong như pha lê, tôm tươi to bóng. Chấm dầu hào + tương ớt... Perfect dim sum Sunday!',
   ARRAY['dimsum','ha_cao','tom','hong_kong','dim_sum','steam'],
   4120, 123, 52, 15400, true),

  (v_t11, m11b,
   '[{"url":"https://images.unsplash.com/photo-1647674879705-1a543f9d8f4a?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Sủi cảo tôm thịt trong nước broth gà trong vắt 🥟 Một bát nhỏ nhưng đủ ấm bụng cho buổi sáng Hà Nội se lạnh. Đơn giản mà đỉnh!',
   ARRAY['sui_cao','wonton','ga','soup','dim_sum','ha_noi'],
   1980, 48, 16, 7100, false),

  -- ── Bánh Xèo Mười Xiềm ───────────────────────────────────
  (v_t12, m12a,
   '[{"url":"https://images.unsplash.com/photo-1625400851892-8e1b4c9bdfbf?w=800&auto=format&fit=crop","layout":"LANDSCAPE"}]',
   'Bánh xèo Mười Xiềm — tiếng XÈOOOO quen thuộc vang khắp ngõ! 🔊🥬 Bột bánh vàng giòn, tôm to lớn, giá đỗ sần sật, ăn cuốn rau sống chấm mắm thì hết sầu đời!',
   ARRAY['banh_xeo','mien_nam','street_food','tom','gion','sai_gon','viet_food'],
   7640, 287, 124, 44800, true),

  (v_t12, m12b,
   '[{"url":"https://images.unsplash.com/photo-1617622141573-c6ea7c06ac50?w=800&auto=format&fit=crop","layout":"PORTRAIT"}]',
   'Bánh Khọt coconut shrimp — em họ của bánh xèo nhưng cute hơn xíu 😄🥥 Từng chiếc bánh nhỏ nhắn béo thơm nước cốt dừa, tôm nằm trên top là điểm nhấn thẩm mỹ.',
   ARRAY['banh_khot','dua','coconut','tom','mien_nam','cute','sai_gon'],
   3890, 98, 44, 14200, false);

END $$;
