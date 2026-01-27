-- Script tạo tài khoản test cho OneOrder_SM
-- Chạy script này trong Supabase SQL Editor

-- Tạo tài khoản Manager
-- Thay 'manager@oneorder.com' và 'password123' bằng email/password bạn muốn
INSERT INTO auth.users (
  email,
  password_hash,
  email_confirmed_at,
  created_at,
  updated_at
) VALUES (
  'manager@oneorder.com',
  crypt('password123', gen_salt('bf', 10)),
  NOW(),
  NOW(),
  NOW()
) RETURNING id;

-- Lấy ID của user vừa tạo và tạo profile với role = 'manager'
-- (Thay 'generated-user-id' bằng ID trả về từ câu lệnh trên)
INSERT INTO profiles (
  id,
  email,
  name,
  role,
  created_at,
  updated_at
) VALUES (
  'generated-user-id',
  'manager@oneorder.com',
  'Manager Account',
  'manager',
  NOW(),
  NOW()
);

-- Tạo tài khoản Staff
INSERT INTO auth.users (
  email,
  password_hash,
  email_confirmed_at,
  created_at,
  updated_at
) VALUES (
  'staff@oneorder.com',
  crypt('password123', gen_salt('bf', 10)),
  NOW(),
  NOW(),
  NOW()
) RETURNING id;

-- Lấy ID của user vừa tạo và tạo profile với role = 'staff'
-- (Thay 'generated-user-id' bằng ID trả về từ câu lệnh trên)
INSERT INTO profiles (
  id,
  email,
  name,
  role,
  created_at,
  updated_at
) VALUES (
  'generated-user-id-2',
  'staff@oneorder.com',
  'Staff Account',
  'staff',
  NOW(),
  NOW()
);