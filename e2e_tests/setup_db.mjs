// setup_db.mjs
// Script này khởi tạo dữ liệu cho Maestro Test chạy.
// Chạy bằng Node.js: node setup_db.mjs

import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';
dotenv.config();

const SUPABASE_URL = process.env.SUPABASE_URL || 'YOUR_SUPABASE_URL';
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY || 'YOUR_SUPABASE_SERVICE_ROLE_KEY';

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

async function setupTestData() {
  console.log("🚀 Bắt đầu tạo dữ liệu E2E Test...");

  // 1. Tạo Tenant (Nhà hàng)
  const tenantId = 'test_tenant_e2e';
  const { error: tError } = await supabase.from('tenants').upsert({
    id: tenantId,
    name: 'Nhà hàng Test E2E',
    address: '123 Đường Test',
    is_active: true
  });
  if (tError) console.error("Lỗi tạo Tenant:", tError);

  // 2. Tạo Bàn ăn
  const tableId = 999;
  const { error: tableError } = await supabase.from('tables').upsert({
    tenant_id: tenantId,
    table_id: tableId,
    capacity: 4,
    location: 'Khu Test',
    status: 'free'
  });
  if (tableError) console.error("Lỗi tạo Bàn:", tableError);

  // 3. Tạo Category
  const categoryId = 999;
  await supabase.from('categories').upsert({
    tenant_id: tenantId,
    id: categoryId,
    name: 'Món Test',
    display_order: 1
  });

  // 4. Tạo Món ăn
  const menuItemId = 999;
  const { error: mError } = await supabase.from('menu_items').upsert({
    tenant_id: tenantId,
    id: menuItemId,
    category_id: categoryId,
    name: 'Cơm Chiên Test',
    description: 'Món ăn dùng để chạy auto test',
    price: 50000,
    is_available: true
  });
  if (mError) console.error("Lỗi tạo Món ăn:", mError);

  // 5. Cài đặt Account Staff Test
  // Lưu ý: User phải được tạo trên Auth trước, hoặc ta dùng 1 user có sẵn
  // Ở đây giả định user staff_test@example.com đã được tạo qua Supabase Dashboard.
  
  console.log(`✅ Setup xong! Tenant ID: ${tenantId}, Table ID: ${tableId}`);
  console.log(`Bây giờ bạn có thể chạy: maestro test full_flow.yaml`);
}

setupTestData();
