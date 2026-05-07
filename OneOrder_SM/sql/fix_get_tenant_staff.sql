-- Fix for get_tenant_staff function
-- Issue: email column returns varchar(255) but function expects TEXT
-- Solution: Cast u.email to TEXT

DROP FUNCTION IF EXISTS public.get_tenant_staff();

CREATE OR REPLACE FUNCTION public.get_tenant_staff()
RETURNS TABLE (
    id UUID,
    full_name TEXT,
    phone_number TEXT,
    role TEXT,
    email TEXT,
    is_active BOOLEAN,
    created_at TIMESTAMPTZ,
    created_by_name TEXT,
    avatar_url TEXT
) AS $$
DECLARE
    v_tenant_id UUID;
    v_role TEXT;
BEGIN
    -- Get caller's tenant and role
    SELECT p.tenant_id, p.role INTO v_tenant_id, v_role
    FROM public.profiles p
    WHERE p.id = auth.uid();
    
    -- Verify caller is manager
    IF v_role != 'manager' THEN
        RAISE EXCEPTION 'Only managers can view staff list';
    END IF;
    
    -- Return staff for this tenant
    RETURN QUERY
    SELECT 
        p.id,
        p.full_name,
        p.phone_number,
        p.role,
        u.email::TEXT,  -- CAST to TEXT to match return type
        p.is_active,
        p.created_at,
        creator.full_name as created_by_name,
        p.avatar_url
    FROM public.profiles p
    JOIN auth.users u ON u.id = p.id
    LEFT JOIN public.profiles creator ON creator.id = p.created_by
    WHERE p.tenant_id = v_tenant_id
    ORDER BY p.created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.get_tenant_staff() TO authenticated;
