-- ============================================================================
-- FIX: Enhanced function to ensure data consistency between profiles and tenants
-- This addresses foreign key constraint issues
-- ============================================================================

-- Enhanced function with better error handling and data consistency
CREATE OR REPLACE FUNCTION public.create_restaurant_account(
    p_restaurant_name TEXT,
    p_address TEXT DEFAULT NULL,
    p_phone TEXT DEFAULT NULL,
    p_email TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_tenant_id UUID;
    v_user_id UUID;
    profile_record RECORD;
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

    -- Update user profile to ensure consistency
    -- First, check if the profile exists
    SELECT * INTO profile_record FROM public.profiles WHERE id = v_user_id;

    IF FOUND THEN
        -- Update existing profile
        UPDATE public.profiles
        SET
            role = 'manager',  -- Ensure the role is set to manager
            tenant_id = v_tenant_id,
            updated_at = now()
        WHERE id = v_user_id;
    ELSE
        -- If profile doesn't exist, create it with manager role and tenant association
        INSERT INTO public.profiles (id, full_name, role, is_active, tenant_id)
        VALUES (
            v_user_id,
            (SELECT COALESCE(raw_user_meta_data->>'full_name', email) FROM auth.users WHERE id = v_user_id),
            'manager',
            true,
            v_tenant_id
        );
    END IF;

    -- Verify the tenant exists and return its ID
    IF NOT EXISTS (SELECT 1 FROM public.tenants WHERE id = v_tenant_id AND owner_id = v_user_id) THEN
        RAISE EXCEPTION 'Tenant creation failed - consistency check failed';
    END IF;

    RETURN v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.create_restaurant_account(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- Also create a procedure to fix existing data issues (using procedure instead of function with RETURNS TABLE)
CREATE OR REPLACE PROCEDURE public.fix_profile_tenant_consistency()
AS $$
DECLARE
    profile_record RECORD;
    fix_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Starting profile/tenant consistency check...';

    -- Fix profiles that have tenant_id but no matching tenant record
    FOR profile_record IN
        SELECT p.id as profile_id, p.tenant_id as bad_tenant_id
        FROM public.profiles p
        LEFT JOIN public.tenants t ON p.tenant_id = t.id
        WHERE p.tenant_id IS NOT NULL AND t.id IS NULL
    LOOP
        -- Reset the bad tenant_id to NULL
        UPDATE public.profiles
        SET tenant_id = NULL, role = 'customer'
        WHERE id = profile_record.profile_id;

        RAISE NOTICE 'Fixed profile %: reset bad tenant_id', profile_record.profile_id;
        fix_count := fix_count + 1;
    END LOOP;

    IF fix_count > 0 THEN
        RAISE NOTICE 'Fixed % profiles with bad tenant references', fix_count;
    ELSE
        RAISE NOTICE 'No profile/tenant inconsistencies found';
    END IF;

    -- Fix profiles where user is restaurant owner but has no tenant
    fix_count := 0;
    FOR profile_record IN
        SELECT au.id as user_id, p.id as profile_id
        FROM auth.users au
        JOIN public.profiles p ON au.id = p.id
        LEFT JOIN public.tenants t ON p.tenant_id = t.id
        WHERE (au.raw_user_meta_data->>'is_restaurant_owner')::boolean IS TRUE
        AND (t.id IS NULL OR p.tenant_id IS NULL)
    LOOP
        -- Update role to manager since user registered as restaurant owner
        UPDATE public.profiles
        SET role = 'manager'
        WHERE id = profile_record.profile_id;

        RAISE NOTICE 'Updated profile % to manager role', profile_record.profile_id;
        fix_count := fix_count + 1;
    END LOOP;

    IF fix_count > 0 THEN
        RAISE NOTICE 'Updated % restaurant owner profiles to manager role', fix_count;
    ELSE
        RAISE NOTICE 'No restaurant owner profiles needed role updates';
    END IF;

    RAISE NOTICE 'Profile/tenant consistency check completed';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Execute the fix procedure to clean up any existing inconsistencies
CALL public.fix_profile_tenant_consistency();

-- Verify the functions were created
SELECT routine_name
FROM information_schema.routines
WHERE routine_name = 'create_restaurant_account';