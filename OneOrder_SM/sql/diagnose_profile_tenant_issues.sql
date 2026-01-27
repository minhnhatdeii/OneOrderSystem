-- ============================================================================
-- DIAGNOSIS: Check for profile/tenant mismatches
-- This will help identify any existing data issues
-- ============================================================================

-- 1. Check if the user has a profile with tenant_id but no matching tenant
SELECT 
    p.id as profile_id,
    p.tenant_id as profile_tenant_id,
    p.role,
    t.id as tenant_id,
    t.restaurant_name
FROM public.profiles p
LEFT JOIN public.tenants t ON p.tenant_id = t.id
WHERE p.tenant_id IS NOT NULL AND t.id IS NULL;

-- 2. Check for users who have is_restaurant_owner flag but no tenant
SELECT 
    au.id as user_id,
    au.email,
    au.raw_user_meta_data->>'is_restaurant_owner' as is_restaurant_owner,
    p.id as profile_id,
    p.tenant_id,
    p.role,
    t.id as tenant_id
FROM auth.users au
JOIN public.profiles p ON au.id = p.id
LEFT JOIN public.tenants t ON p.tenant_id = t.id
WHERE (au.raw_user_meta_data->>'is_restaurant_owner')::boolean IS TRUE
AND (t.id IS NULL OR p.tenant_id IS NULL);

-- 3. Check for users with manager role but no tenant
SELECT 
    p.id as profile_id,
    p.role,
    p.tenant_id,
    au.email,
    au.raw_user_meta_data
FROM public.profiles p
JOIN auth.users au ON p.id = au.id
WHERE p.role = 'manager' AND p.tenant_id IS NULL;