-- Fix RLS permissions and ensure Manager profile exists
-- Run this in Supabase SQL Editor

-- 1. Ensure public.profiles is readable
-- The RLS policy on 'tables' needs to query 'profiles' to check the role.
-- If the user can't read 'profiles', the check fails.
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Public profiles are viewable by everyone" 
ON public.profiles FOR SELECT 
USING ( true );

-- 2. Ensure the Manager user exists in public.profiles with correct role.
-- User ID from your error log: 614b2558-d880-4a6c-8b88-3a96e45543a7
INSERT INTO public.profiles (id, email, role, full_name)
VALUES (
  '614b2558-d880-4a6c-8b88-3a96e45543a7', 
  'manager@oneorder.com', 
  'manager', 
  'Manager Account'
)
ON CONFLICT (id) DO UPDATE 
SET role = 'manager'; -- Ensure role is definitely 'manager'

-- 3. Just in case, ensure managers can Update/Insert profiles (optional but good)
DROP POLICY IF EXISTS "Managers can update profiles" ON public.profiles;
CREATE POLICY "Managers can update profiles" 
ON public.profiles FOR UPDATE 
USING ( auth.uid() = id );
