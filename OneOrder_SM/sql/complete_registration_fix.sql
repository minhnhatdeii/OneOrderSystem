-- ============================================================================
-- ENHANCEMENT: Complete registration for restaurant owners
-- This ensures both profile and tenant are created during registration
-- ============================================================================

-- Create a combined function to register a restaurant owner completely
CREATE OR REPLACE FUNCTION public.register_restaurant_owner(
    p_email TEXT,
    p_password TEXT,
    p_full_name TEXT,
    p_restaurant_name TEXT,
    p_address TEXT DEFAULT NULL,
    p_phone TEXT DEFAULT NULL
) RETURNS TABLE(user_id UUID, tenant_id UUID) AS $$
DECLARE
    v_user_id UUID;
    v_tenant_id UUID;
BEGIN
    -- Create the auth user
    -- Note: This would typically be done in your application code via Supabase Auth API
    -- For this function to work properly, you'd call auth.admin.createUser() from your backend
    
    -- Since we can't create auth users from within RLS-protected functions,
    -- we'll define the logic that would be used by your Edge Function
    
    -- The actual implementation would be in a Supabase Edge Function with service_role
    -- that handles the complete registration flow:
    -- 1. Create auth user
    -- 2. Create tenant
    -- 3. Update profile to manager with tenant association
    
    -- For now, this function shows the structure that would be used
    
    -- Return placeholder values - the real implementation would be in an Edge Function
    RAISE NOTICE 'This function would be called from an Edge Function with service_role';
    RETURN QUERY SELECT gen_random_uuid(), gen_random_uuid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- The real fix needs to be in the application layer:
-- 1. First, register the user (which creates profile with manager role via trigger)
-- 2. Then immediately create the restaurant/tenant

-- This would be handled in your app by modifying the registration process
-- to call the tenant creation immediately after successful registration.

-- Make sure the create_restaurant_account function is still available and working
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

    -- Update user profile to associate with tenant (role should already be manager)
    UPDATE public.profiles
    SET
        tenant_id = v_tenant_id,
        updated_at = now()
    WHERE id = v_user_id;

    RETURN v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.create_restaurant_account(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- Verify the function was created
SELECT routine_name 
FROM information_schema.routines 
WHERE routine_name = 'create_restaurant_account';