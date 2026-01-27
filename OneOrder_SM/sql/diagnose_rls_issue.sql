-- Diagnostic script to check RLS setup for tables
-- Run this in Supabase SQL Editor to see what's wrong

-- 1. Check if the manager profile exists and has correct role
SELECT 
    id, 
    role, 
    full_name,
    phone_number
FROM public.profiles 
WHERE id = '614b2558-d880-4a6c-8b88-3a96e45543a7';

-- 2. Check all RLS policies on the tables table
SELECT 
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies 
WHERE tablename = 'tables';

-- 3. Check if RLS is enabled on tables
SELECT 
    tablename, 
    rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' AND tablename = 'tables';

-- 4. Check if RLS is enabled on profiles
SELECT 
    tablename, 
    rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' AND tablename = 'profiles';

-- 5. Test if current user can read profiles (run this while logged in as manager)
-- This should return rows if the policy works
SELECT COUNT(*) as profile_count
FROM public.profiles 
WHERE role = 'manager';
