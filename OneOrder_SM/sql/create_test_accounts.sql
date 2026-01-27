-- Create Test Accounts for OneOrder_SM
-- Note: You need to run this in Supabase SQL Editor with proper permissions

-- First, create the users in auth.users table
-- Password for both accounts: "Test123456"
-- You'll need to use Supabase Dashboard > Authentication > Users > Add User
-- Or use Supabase Auth Admin API

-- After creating users via Dashboard, run this to set their roles:

-- Example: Replace these UUIDs with actual user IDs from auth.users after creation
-- User 1: staff@oneorder.com (STAFF)
-- User 2: manager@oneorder.com (MANAGER)

-- Update profiles for staff account
-- Replace 'STAFF_USER_UUID' with actual UUID from auth.users
UPDATE public.profiles 
SET 
    role = 'staff',
    full_name = 'Test Staff',
    phone_number = '0123456789'
WHERE id = 'STAFF_USER_UUID';

-- Update profiles for manager account  
-- Replace 'MANAGER_USER_UUID' with actual UUID from auth.users
UPDATE public.profiles 
SET 
    role = 'manager',
    full_name = 'Test Manager',
    phone_number = '0987654321'
WHERE id = 'MANAGER_USER_UUID';

-- Verify the accounts
SELECT id, email, role, full_name 
FROM public.profiles 
WHERE role IN ('staff', 'manager');
