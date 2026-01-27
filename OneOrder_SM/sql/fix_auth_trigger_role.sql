-- ============================================================================
-- FIX: Auth trigger to assign correct role based on registration data
-- This fixes the issue where restaurant owners get "customer" role instead of "manager"
-- Run this in Supabase SQL Editor
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

-- Drop and recreate the create_restaurant_account function to ensure it works properly
-- This function should create the tenant and update the user's profile to associate with it
CREATE OR REPLACE FUNCTION public.create_restaurant_account(
    p_restaurant_name TEXT,
    p_address TEXT DEFAULT NULL,
    p_phone TEXT DEFAULT NULL,
    p_email TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_tenant_id UUID;
    v_user_id UUID;
    profile_exists BOOLEAN;
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

    -- Update user profile - ensure it has manager role and tenant association
    UPDATE public.profiles
    SET
        role = 'manager',  -- Ensure the role is set to manager
        tenant_id = v_tenant_id,
        updated_at = now()
    WHERE id = v_user_id;

    -- Check if the update affected any rows
    IF NOT FOUND THEN
        -- If profile doesn't exist, create it (this should only happen if trigger failed)
        INSERT INTO public.profiles (id, full_name, role, is_active, tenant_id)
        VALUES (
            v_user_id,
            (SELECT COALESCE(raw_user_meta_data->>'full_name', email) FROM auth.users WHERE id = v_user_id),
            'manager',
            true,
            v_tenant_id
        );
    END IF;

    RETURN v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.create_restaurant_account(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- Verify the functions were created
SELECT routine_name
FROM information_schema.routines
WHERE routine_name IN ('handle_new_user', 'create_restaurant_account');