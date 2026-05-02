-- ============================================================
-- FILE: 10_more_mock_data_xuan_thuy.sql
-- MỤC ĐÍCH: Thêm 10 nhà hàng & 20 bài feed quanh 144 Xuân Thủy
--           để xử lý lỗi lặp lại feed do thiếu data.
-- ============================================================

DO $$
DECLARE
  v_owner UUID;
  v_t1 UUID := gen_random_uuid();
  v_t2 UUID := gen_random_uuid();
  v_t3 UUID := gen_random_uuid();
  v_t4 UUID := gen_random_uuid();
  v_t5 UUID := gen_random_uuid();
  v_t6 UUID := gen_random_uuid();
  v_t7 UUID := gen_random_uuid();
  v_t8 UUID := gen_random_uuid();
  v_t9 UUID := gen_random_uuid();
  v_t10 UUID := gen_random_uuid();

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
BEGIN
  -- Lấy user có sẵn làm owner
  SELECT id INTO v_owner FROM auth.users LIMIT 1;
  IF v_owner IS NULL THEN
    v_owner := gen_random_uuid();
  END IF;

  -- ══════════════════════════════════════════════════════════
  --  INSERT 10 TENANTS QUANH KHU VỰC 144 XUÂN THỦY, CẦU GIẤY
  --  (Toạ độ tham chiếu: ~21.0366, 105.7818)
  -- ══════════════════════════════════════════════════════════
  INSERT INTO tenants (id, owner_id, restaurant_name, address, latitude, longitude, avatar_url, created_at, updated_at) VALUES
    (v_t1, v_owner, 'Bún Chả Nem Nướng Cầu Giấy', '140 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0365, 105.7810, 'https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t2, v_owner, 'Trà Sữa Feel Good', '148 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0368, 105.7820, 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t3, v_owner, 'KFC Xuân Thủy', '150 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0370, 105.7822, 'https://images.unsplash.com/photo-1513639776629-7b61b0ac49cb?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t4, v_owner, 'Cơm Tấm Sườn Bì Xuân Thủy', 'Ngõ 130 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0358, 105.7805, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t5, v_owner, 'Phở Cuốn Hương Mai', '144 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0366, 105.7818, 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t6, v_owner, 'Bánh Mì Chảo Cô Long', 'Ngõ 144 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0367, 105.7819, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t7, v_owner, 'Lẩu Nướng Sinh Viên', 'Trần Thái Tông, Cầu Giấy, Hà Nội', 21.0350, 105.7830, 'https://images.unsplash.com/photo-1534482421-64566f976cfa?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t8, v_owner, 'Highlands Coffee Xuân Thủy', '122 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0355, 105.7800, 'https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t9, v_owner, 'Bít Tết Ngọc Hiếu', '160 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0380, 105.7835, 'https://images.unsplash.com/photo-1558030006-450675393462?w=500&auto=format&fit=crop', NOW(), NOW()),
    (v_t10, v_owner, 'Nem Nướng Nha Trang', 'Ngõ 130 Xuân Thủy, Cầu Giấy, Hà Nội', 21.0360, 105.7808, 'https://images.unsplash.com/photo-1614251055880-ee96e4803393?w=500&auto=format&fit=crop', NOW(), NOW());

  -- ══════════════════════════════════════════════════════════
  --  INSERT MENU ITEMS (2 món / nhà hàng)
  -- ══════════════════════════════════════════════════════════
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t1, 'Bún Chả Nước', 40000, true) RETURNING id INTO m1a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t1, 'Nem Rán', 10000, true) RETURNING id INTO m1b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t2, 'Trà Sữa Chân Trâu', 35000, true) RETURNING id INTO m2a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t2, 'Trà Đào Cam Sả', 40000, true) RETURNING id INTO m2b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t3, 'Gà Rán Truyền Thống', 35000, true) RETURNING id INTO m3a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t3, 'Khoai Tây Chiên', 20000, true) RETURNING id INTO m3b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t4, 'Cơm Tấm Sườn Nướng', 45000, true) RETURNING id INTO m4a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t4, 'Cơm Tấm Đùi Gà', 45000, true) RETURNING id INTO m4b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t5, 'Phở Cuốn (10 cái)', 65000, true) RETURNING id INTO m5a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t5, 'Phở Chiên Phồng', 70000, true) RETURNING id INTO m5b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t6, 'Bánh Mì Chảo Đầy Đủ', 45000, true) RETURNING id INTO m6a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t6, 'Bánh Mì Sốt Vang', 50000, true) RETURNING id INTO m6b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t7, 'Buffet Lẩu Nướng', 139000, true) RETURNING id INTO m7a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t7, 'Ba Chỉ Bò Mỹ Nướng', 89000, true) RETURNING id INTO m7b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t8, 'Trà Sen Vàng', 45000, true) RETURNING id INTO m8a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t8, 'Phindi Hạnh Nhân', 49000, true) RETURNING id INTO m8b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t9, 'Bít Tết Truyền Thống', 120000, true) RETURNING id INTO m9a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t9, 'Mì Ý Xốt Bò Băm', 90000, true) RETURNING id INTO m9b;

  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t10, 'Nem Nướng Nha Trang (Phần 1 người)', 35000, true) RETURNING id INTO m10a;
  INSERT INTO menu_items (tenant_id, name, price, is_available) VALUES (v_t10, 'Bún Nem Nướng', 40000, true) RETURNING id INTO m10b;

  -- ══════════════════════════════════════════════════════════
  --  INSERT FOOD POSTS (20 posts)
  -- ══════════════════════════════════════════════════════════
  INSERT INTO food_posts (tenant_id, menu_item_id, images, caption, category_tags, like_count, comment_count, share_count, view_count, is_trending) VALUES
  (v_t1, m1a, '[{"url":"https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=800","layout":"PORTRAIT"}]', 'Bún chả Cầu Giấy ngon đỉnh chóp, thịt nướng than hoa thơm lừng. Phù hợp sinh viên!', ARRAY['bun_cha','cau_giay','an_trua'], 1200, 30, 5, 5200, false),
  (v_t1, m1b, '[{"url":"https://images.unsplash.com/photo-1614251055880-ee96e4803393?w=800","layout":"SQUARE"}]', 'Nem rán giòn rụm tại Xuân Thủy, must try cho anh em bách khoa báo chí nhé!', ARRAY['nem_ran','gion','an_vat'], 850, 15, 2, 4100, false),

  (v_t2, m2a, '[{"url":"https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=800","layout":"PORTRAIT"}]', 'Trà sữa trân châu đường đen bao phê, ngõ 148 Xuân Thuỷ gọi tên!', ARRAY['tra_sua','duong_den','sinh_vien'], 2100, 56, 12, 8900, true),
  (v_t2, m2b, '[{"url":"https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=800","layout":"LANDSCAPE"}]', 'Trà đào cam sả thanh mát giải nhiệt mùa hè nè các tín đồ nghiện trà ơi!', ARRAY['tra_dao','thanh_mat','nuoc'], 1500, 20, 4, 6000, false),

  (v_t3, m3a, '[{"url":"https://images.unsplash.com/photo-1513639776629-7b61b0ac49cb?w=800","layout":"PORTRAIT"}]', 'Gà rán KFC lúc nào cũng là chân ái sau mỗi giờ nộp bài tập lớn 😂', ARRAY['ga_ran','fast_food','kfc'], 5400, 150, 40, 15000, true),
  (v_t3, m3b, '[{"url":"https://images.unsplash.com/photo-1576107232684-1279f39085ac?w=800","layout":"SQUARE"}]', 'Khoai tây chiên giòn tan, nhâm nhi cả buổi chiều học bài không chán.', ARRAY['khoai_tay_chien','fast_food'], 1100, 12, 1, 3500, false),

  (v_t4, m4a, '[{"url":"https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=800","layout":"PORTRAIT"}]', 'Cơm tấm sườn nướng siêu to khổng lồ ngay ngõ 130 Xuân Thủy.', ARRAY['com_tam','suon_nuong','no_bung'], 3120, 80, 15, 12000, true),
  (v_t4, m4b, '[{"url":"https://images.unsplash.com/photo-1598514983318-2f64f8f4796c?w=800","layout":"PORTRAIT"}]', 'Cơm tấm đùi gà nướng mật ong thơm phức, hạt cơm mềm dẻo. Tuyệt trần!', ARRAY['com_tam','ga_nuong','an_trua'], 2500, 45, 10, 9500, false),

  (v_t5, m5a, '[{"url":"https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800","layout":"LANDSCAPE"}]', 'Phở cuốn dai dai, bò xào đậm vị, nước mắm pha chua ngọt siêu dính ngón 144 Xuân Thủy.', ARRAY['pho_cuon','bo_xao','an_vat'], 4200, 120, 25, 14500, true),
  (v_t5, m5b, '[{"url":"https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800","layout":"PORTRAIT"}]', 'Bát phở chiên phồng béo ngậy, ngoài giòn trong mềm, ăn 1 đĩa no cả ngày.', ARRAY['pho_chien','ha_noi','cay'], 2800, 60, 18, 10200, false),

  (v_t6, m6a, '[{"url":"https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800","layout":"PORTRAIT"}]', 'Bánh mì chảo full topping xúc xích, pate, trứng ốp la béo ngậy. Ngon bổ rẻ!', ARRAY['banh_mi_chao','an_sang','sinh_vien'], 3500, 90, 20, 13000, true),
  (v_t6, m6b, '[{"url":"https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800","layout":"SQUARE"}]', 'Sáng mùa đông ra đá chảo bánh mì sốt vang nóng hổi thì còn gì bằng.', ARRAY['banh_mi','sot_vang','am_ap'], 2100, 50, 10, 8000, false),

  (v_t7, m7a, '[{"url":"https://images.unsplash.com/photo-1534482421-64566f976cfa?w=800","layout":"LANDSCAPE"}]', 'Buffet nướng lẩu thả ga chỉ 139k/người cho sinh viên tại phố Trần Thái Tông.', ARRAY['buffet','lau_nuong','sinh_vien'], 6500, 250, 80, 22000, true),
  (v_t7, m7b, '[{"url":"https://images.unsplash.com/photo-1544025162-d76694265947?w=800","layout":"PORTRAIT"}]', 'Thịt bò Mỹ nướng tảng cực mọng nước rưới sốt béo ngậy ai mà cưỡng lại được.', ARRAY['thit_nuong','bbq','bo_my'], 4200, 110, 35, 16000, false),

  (v_t8, m8a, '[{"url":"https://images.unsplash.com/photo-1622485542289-498c8c560241?w=800","layout":"PORTRAIT"}]', 'Trà sen vàng macchiato béo ngậy, signature của nhà Highlands chưa bao giờ hết hot.', ARRAY['tra_sen_vang','highlands','cafe'], 5100, 180, 40, 18000, true),
  (v_t8, m8b, '[{"url":"https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800","layout":"SQUARE"}]', 'Trải nghiệm phindi hạnh nhân mới lạ, thơm bùi vị cà phê. Quán Xuân Thuỷ siêu rộng rãi.', ARRAY['phindi','cafe','highlands'], 2900, 75, 15, 10500, false),

  (v_t9, m9a, '[{"url":"https://images.unsplash.com/photo-1558030006-450675393462?w=800","layout":"PORTRAIT"}]', 'Bít tết chuẩn vị Việt ngay Xuân Thuỷ, 1 phần quá trời thịt và khoai.', ARRAY['bit_tet','thit_bo','ngoc_hieu'], 3800, 95, 22, 12500, true),
  (v_t9, m9b, '[{"url":"https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=800","layout":"PORTRAIT"}]', 'Mì Ý sốt bò băm đậm đà, phô mai ngập ngụa, làm ngay một suất cho ấm bụng.', ARRAY['mi_y','pasta','pho_mai'], 2400, 40, 8, 8500, false),

  (v_t10, m10a, '[{"url":"https://images.unsplash.com/photo-1614251055880-ee96e4803393?w=800","layout":"LANDSCAPE"}]', 'Nem nướng Nha Trang thơm lừng, cuốn bánh tráng rau sống, chấm nước lèo béo béo.', ARRAY['nem_nuong','nha_trang','cuon'], 4900, 140, 38, 17000, true),
  (v_t10, m10b, '[{"url":"https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=800","layout":"PORTRAIT"}]', 'Đã thèm nem nướng thì sao có thể bỏ qua bún nem nướng cực chất lượng ở ngõ 130.', ARRAY['bun','nem_nuong','an_trua'], 3200, 85, 19, 11000, false);

END $$;
