-- ============================================================================
-- FIX: Auth trigger to assign correct role based on registration data
-- This fixes the issue where restaurant owners get "customer" role instead of "manager"
-- Run this in Supabase SQL Editor FIRST
-- ============================================================================

-- Update the handle_new_user function to check for restaurant owner flag
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
    user_role TEXT;
BEGIN
    -- Check if user has is_restaurant_owner flag set to true during registration
    -- If the user is registering as a restaurant owner, assign 'manager' role from the start
    IF (new.raw_user_meta_data->>'is_restaurant_owner')::boolean IS TRUE THEN
        user_role := 'manager';
    ELSE
        -- Default to customer for regular users
        user_role := COALESCE(new.raw_user_meta_data->>'role', 'customer');
    END IF;
    
    INSERT INTO public.profiles (id, full_name, role, is_active)
    VALUES (
        new.id,
        COALESCE(new.raw_user_meta_data->>'full_name', new.email),
        user_role,
        true
    );
    
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Make sure trigger exists and uses the updated function
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Verify the function was created
SELECT routine_name 
FROM information_schema.routines 
WHERE routine_name = 'handle_new_user';