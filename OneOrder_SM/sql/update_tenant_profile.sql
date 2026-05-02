-- Run this script in your Supabase SQL Editor to add cover_url and description to the tenants table
ALTER TABLE public.tenants
ADD COLUMN IF NOT EXISTS cover_url TEXT,
ADD COLUMN IF NOT EXISTS description TEXT;

-- Update get_tenant_info function to return new fields
DROP FUNCTION IF EXISTS public.get_tenant_info();
CREATE OR REPLACE FUNCTION public.get_tenant_info()
RETURNS TABLE (
    id UUID,
    restaurant_name TEXT,
    business_type TEXT,
    address TEXT,
    phone_number TEXT,
    email TEXT,
    logo_url TEXT,
    cover_url TEXT,
    description TEXT,
    timezone TEXT,
    currency TEXT,
    is_active BOOLEAN,
    created_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.id,
        t.restaurant_name,
        t.business_type,
        t.address,
        t.phone_number,
        t.email,
        t.logo_url,
        t.cover_url,
        t.description,
        t.timezone,
        t.currency,
        t.is_active,
        t.created_at
    FROM public.tenants t
    WHERE t.id = public.get_user_tenant_id();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_tenant_info() TO authenticated;

-- Update update_tenant_info function to accept new fields
DROP FUNCTION IF EXISTS public.update_tenant_info(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.update_tenant_info(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT);

CREATE OR REPLACE FUNCTION public.update_tenant_info(
    p_restaurant_name TEXT DEFAULT NULL,
    p_address TEXT DEFAULT NULL,
    p_phone TEXT DEFAULT NULL,
    p_email TEXT DEFAULT NULL,
    p_logo_url TEXT DEFAULT NULL,
    p_business_type TEXT DEFAULT NULL,
    p_cover_url TEXT DEFAULT NULL,
    p_description TEXT DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'User does not have a tenant';
    END IF;
    
    -- Verify user is the owner
    IF NOT EXISTS (
        SELECT 1 FROM public.tenants 
        WHERE id = v_tenant_id AND owner_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'Only the restaurant owner can update tenant info';
    END IF;
    
    UPDATE public.tenants
    SET 
        restaurant_name = COALESCE(p_restaurant_name, restaurant_name),
        address = COALESCE(p_address, address),
        phone_number = COALESCE(p_phone, phone_number),
        email = COALESCE(p_email, email),
        logo_url = COALESCE(p_logo_url, logo_url),
        business_type = COALESCE(p_business_type, business_type),
        cover_url = COALESCE(p_cover_url, cover_url),
        description = COALESCE(p_description, description),
        updated_at = now()
    WHERE id = v_tenant_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.update_tenant_info(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;
