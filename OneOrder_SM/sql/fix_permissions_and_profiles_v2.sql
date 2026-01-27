-- Fix RLS permissions and ensure Manager profile exists (Version 2)
-- Run this in Supabase SQL Editor

-- 1. Ensure public.profiles is readable
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Public profiles are viewable by everyone" 
ON public.profiles FOR SELECT 
USING ( true );

-- 2. Ensure the Manager user exists in public.profiles with correct role.
-- Note: 'email' column often doesn't exist in public.profiles as it's in auth.users
-- We will only update/insert id, role, full_name

INSERT INTO public.profiles (id, role, full_name)
VALUES (
  '614b2558-d880-4a6c-8b88-3a96e45543a7', 
  'manager', 
  'Manager Account'
)
ON CONFLICT (id) DO UPDATE 
SET role = 'manager'; 

-- 3. Ensure managers can Update/Insert their own profiles
DROP POLICY IF EXISTS "Managers can update profiles" ON public.profiles;
CREATE POLICY "Managers can update profiles" 
ON public.profiles FOR UPDATE 
USING ( auth.uid() = id );
