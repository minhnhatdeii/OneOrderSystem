-- Complete fix for RLS on tables - Run this entire script
-- This will ensure everything is set up correctly

-- Step 1: Make sure profiles table allows reading by authenticated users
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Drop existing policies to start fresh
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
DROP POLICY IF EXISTS "Users can view all profiles" ON public.profiles;

-- Create a policy that allows authenticated users to read all profiles
-- This is needed because the tables RLS policy needs to query profiles
CREATE POLICY "Authenticated users can view all profiles" 
ON public.profiles FOR SELECT 
TO authenticated
USING ( true );

-- Step 2: Ensure the manager profile exists with correct role
INSERT INTO public.profiles (id, role, full_name)
VALUES (
  '614b2558-d880-4a6c-8b88-3a96e45543a7', 
  'manager', 
  'Manager Account'
)
ON CONFLICT (id) DO UPDATE 
SET role = 'manager',
    full_name = COALESCE(profiles.full_name, 'Manager Account');

-- Step 3: Fix the tables RLS policies
-- Drop all existing policies on tables
DROP POLICY IF EXISTS "Managers full access to tables" ON public.tables;
DROP POLICY IF EXISTS "Staff can view tables" ON public.tables;
DROP POLICY IF EXISTS "Staff can update table status" ON public.tables;
DROP POLICY IF EXISTS "Customers can view tables" ON public.tables;

-- Recreate the manager policy with explicit role check
CREATE POLICY "Managers full access to tables"
ON public.tables
FOR ALL
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.profiles 
    WHERE profiles.id = auth.uid() 
    AND profiles.role = 'manager'
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.profiles 
    WHERE profiles.id = auth.uid() 
    AND profiles.role = 'manager'
  )
);

-- Add policies for staff
CREATE POLICY "Staff can view tables"
ON public.tables
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.profiles 
    WHERE profiles.id = auth.uid() 
    AND profiles.role IN ('staff', 'manager')
  )
);

CREATE POLICY "Staff can update table status"
ON public.tables
FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.profiles 
    WHERE profiles.id = auth.uid() 
    AND profiles.role IN ('staff', 'manager')
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.profiles 
    WHERE profiles.id = auth.uid() 
    AND profiles.role IN ('staff', 'manager')
  )
);

-- Step 4: Verify the setup
SELECT 'Profile check:' as step, id, role, full_name 
FROM public.profiles 
WHERE id = '614b2558-d880-4a6c-8b88-3a96e45543a7';
