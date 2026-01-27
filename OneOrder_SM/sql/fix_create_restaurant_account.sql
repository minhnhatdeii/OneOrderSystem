-- ============================================================================
-- FIX: Create Restaurant Account Function
-- This script fixes the issue where role stays 'customer' after registration
-- Run this in Supabase SQL Editor
-- ============================================================================

-- Drop and recreate the function with explicit permissions bypass
CREATE OR REPLACE FUNCTION public.create_restaurant_account(
    p_restaurant_name TEXT,
    p_address TEXT DEFAULT NULL,
    p_phone TEXT DEFAULT NULL,
    p_email TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_tenant_id UUID;
    v_user_id UUID;
BEGIN
    -- Get current user ID
    v_user_id := auth.uid();
    
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;
    
    -- Check if user already has a tenant
    IF EXISTS (SELECT 1 FROM public.profiles WHERE id = v_user_id AND tenant_id IS NOT NULL) THEN
        RAISE EXCEPTION 'User already has a restaurant';
    END IF;
    
    -- Create tenant
    INSERT INTO public.tenants (
        owner_id, 
        restaurant_name, 
        address, 
        phone_number, 
        email
    )
    VALUES (
        v_user_id, 
        p_restaurant_name, 
        p_address, 
        p_phone, 
        p_email
    )
    RETURNING id INTO v_tenant_id;
    
    -- Update user profile to manager role and assign tenant
    -- IMPORTANT: This must update the role from 'customer' to 'manager'
    UPDATE public.profiles
    SET 
        role = 'manager',
        tenant_id = v_tenant_id,
        updated_at = now()
    WHERE id = v_user_id;
    
    -- Verify the update was successful
    IF NOT FOUND THEN
        -- If profile doesn't exist, create it
        INSERT INTO public.profiles (id, role, tenant_id, is_active)
        VALUES (v_user_id, 'manager', v_tenant_id, true);
    END IF;
    
    RETURN v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.create_restaurant_account(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- ============================================================================
-- FIX: RLS Policy for Profiles UPDATE
-- Make sure user can update their own profile (or function can via SECURITY DEFINER)
-- ============================================================================

-- Drop existing UPDATE policy if any
DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;

-- Allow users to update their own profile
CREATE POLICY "profiles_update_own" ON public.profiles
    FOR UPDATE
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

-- Also ensure the trigger for handle_new_user works correctly
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, role, is_active)
  VALUES (
    new.id,
    new.raw_user_meta_data->>'full_name',
    COALESCE(new.raw_user_meta_data->>'role', 'customer'),  -- Default to customer
    true
  );
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Make sure trigger exists
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- VERIFICATION: Test the function
-- Run this to check if the function works
-- ============================================================================

-- Check current user's profile role:
-- SELECT id, role, tenant_id FROM public.profiles WHERE id = auth.uid();

SELECT 'Script executed successfully. Please test registration again.' as status;
