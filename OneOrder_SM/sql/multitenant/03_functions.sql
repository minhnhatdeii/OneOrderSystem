-- ============================================================================
-- MULTI-TENANT DATABASE FUNCTIONS
-- OneOrder_SM - Step 3: Create functions for tenant and staff management
-- Run this script AFTER 02_rls_policies.sql
-- ============================================================================

-- ============================================================================
-- FUNCTION: Create Restaurant Account
-- Called when a new user registers their restaurant
-- ============================================================================

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
    UPDATE public.profiles
    SET 
        role = 'manager',
        tenant_id = v_tenant_id,
        updated_at = now()
    WHERE id = v_user_id;
    
    RETURN v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.create_restaurant_account(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- ============================================================================
-- FUNCTION: Get Tenant Info
-- Returns the current user's tenant information
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_tenant_info()
RETURNS TABLE (
    id UUID,
    restaurant_name TEXT,
    business_type TEXT,
    address TEXT,
    phone_number TEXT,
    email TEXT,
    logo_url TEXT,
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
        t.timezone,
        t.currency,
        t.is_active,
        t.created_at
    FROM public.tenants t
    WHERE t.id = public.get_user_tenant_id();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_tenant_info() TO authenticated;

-- ============================================================================
-- FUNCTION: Update Tenant Info
-- Managers can update their restaurant information
-- ============================================================================

CREATE OR REPLACE FUNCTION public.update_tenant_info(
    p_restaurant_name TEXT DEFAULT NULL,
    p_address TEXT DEFAULT NULL,
    p_phone TEXT DEFAULT NULL,
    p_email TEXT DEFAULT NULL,
    p_logo_url TEXT DEFAULT NULL,
    p_business_type TEXT DEFAULT NULL
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
        updated_at = now()
    WHERE id = v_tenant_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.update_tenant_info(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- ============================================================================
-- FUNCTION: Get Staff List
-- Managers can view all staff in their tenant
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_tenant_staff()
RETURNS TABLE (
    id UUID,
    full_name TEXT,
    phone_number TEXT,
    role TEXT,
    email TEXT,
    is_active BOOLEAN,
    created_at TIMESTAMPTZ,
    created_by_name TEXT
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
        creator.full_name as created_by_name
    FROM public.profiles p
    JOIN auth.users u ON u.id = p.id
    LEFT JOIN public.profiles creator ON creator.id = p.created_by
    WHERE p.tenant_id = v_tenant_id
    ORDER BY p.created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.get_tenant_staff() TO authenticated;

-- ============================================================================
-- FUNCTION: Deactivate Staff
-- Managers can deactivate staff accounts
-- ============================================================================

CREATE OR REPLACE FUNCTION public.deactivate_staff(p_staff_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_tenant_id UUID;
    v_staff_tenant_id UUID;
    v_role TEXT;
BEGIN
    -- Get caller's tenant and role
    SELECT p.tenant_id, p.role INTO v_tenant_id, v_role
    FROM public.profiles p
    WHERE p.id = auth.uid();
    
    -- Verify caller is manager
    IF v_role != 'manager' THEN
        RAISE EXCEPTION 'Only managers can deactivate staff';
    END IF;
    
    -- Get staff's tenant
    SELECT tenant_id INTO v_staff_tenant_id
    FROM public.profiles
    WHERE id = p_staff_id;
    
    -- Verify staff belongs to caller's tenant
    IF v_staff_tenant_id != v_tenant_id THEN
        RAISE EXCEPTION 'Staff does not belong to your tenant';
    END IF;
    
    -- Prevent self-deactivation
    IF p_staff_id = auth.uid() THEN
        RAISE EXCEPTION 'Cannot deactivate your own account';
    END IF;
    
    -- Deactivate staff
    UPDATE public.profiles
    SET 
        is_active = false,
        updated_at = now()
    WHERE id = p_staff_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.deactivate_staff(UUID) TO authenticated;

-- ============================================================================
-- FUNCTION: Reactivate Staff
-- Managers can reactivate deactivated staff accounts
-- ============================================================================

CREATE OR REPLACE FUNCTION public.reactivate_staff(p_staff_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_tenant_id UUID;
    v_staff_tenant_id UUID;
    v_role TEXT;
BEGIN
    -- Get caller's tenant and role
    SELECT p.tenant_id, p.role INTO v_tenant_id, v_role
    FROM public.profiles p
    WHERE p.id = auth.uid();
    
    -- Verify caller is manager
    IF v_role != 'manager' THEN
        RAISE EXCEPTION 'Only managers can reactivate staff';
    END IF;
    
    -- Get staff's tenant
    SELECT tenant_id INTO v_staff_tenant_id
    FROM public.profiles
    WHERE id = p_staff_id;
    
    -- Verify staff belongs to caller's tenant
    IF v_staff_tenant_id != v_tenant_id THEN
        RAISE EXCEPTION 'Staff does not belong to your tenant';
    END IF;
    
    -- Reactivate staff
    UPDATE public.profiles
    SET 
        is_active = true,
        updated_at = now()
    WHERE id = p_staff_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.reactivate_staff(UUID) TO authenticated;

-- ============================================================================
-- FUNCTION: Get Order Statistics (Multi-Tenant)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_order_statistics(
    p_start_date DATE DEFAULT CURRENT_DATE - INTERVAL '30 days',
    p_end_date DATE DEFAULT CURRENT_DATE
) RETURNS TABLE (
    order_date DATE,
    total_orders BIGINT,
    total_revenue NUMERIC,
    pending_orders BIGINT,
    completed_orders BIGINT,
    cancelled_orders BIGINT
) AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'User does not have a tenant';
    END IF;
    
    RETURN QUERY
    SELECT 
        o.created_at::DATE as order_date,
        COUNT(*)::BIGINT as total_orders,
        COALESCE(SUM(o.total_amount), 0) as total_revenue,
        COUNT(*) FILTER (WHERE o.status = 'pending')::BIGINT as pending_orders,
        COUNT(*) FILTER (WHERE o.status IN ('served', 'paid'))::BIGINT as completed_orders,
        COUNT(*) FILTER (WHERE o.status = 'cancelled')::BIGINT as cancelled_orders
    FROM public.orders o
    WHERE o.tenant_id = v_tenant_id
    AND o.created_at::DATE BETWEEN p_start_date AND p_end_date
    GROUP BY o.created_at::DATE
    ORDER BY order_date DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_order_statistics(DATE, DATE) TO authenticated;

-- ============================================================================
-- FUNCTION: Get Popular Items (Multi-Tenant)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_popular_items(
    p_limit INTEGER DEFAULT 10,
    p_start_date DATE DEFAULT CURRENT_DATE - INTERVAL '30 days',
    p_end_date DATE DEFAULT CURRENT_DATE
) RETURNS TABLE (
    menu_item_id INTEGER,
    item_name TEXT,
    category_name TEXT,
    total_quantity BIGINT,
    total_revenue NUMERIC
) AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'User does not have a tenant';
    END IF;
    
    RETURN QUERY
    SELECT 
        mi.id as menu_item_id,
        mi.name as item_name,
        c.name as category_name,
        SUM(oi.quantity)::BIGINT as total_quantity,
        SUM(oi.price_at_time * oi.quantity) as total_revenue
    FROM public.order_items oi
    JOIN public.orders o ON o.id = oi.order_id
    JOIN public.menu_items mi ON mi.id = oi.menu_item_id
    JOIN public.categories c ON c.id = mi.category_id
    WHERE o.tenant_id = v_tenant_id
    AND o.created_at::DATE BETWEEN p_start_date AND p_end_date
    AND o.status NOT IN ('cancelled')
    GROUP BY mi.id, mi.name, c.name
    ORDER BY total_quantity DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_popular_items(INTEGER, DATE, DATE) TO authenticated;

-- ============================================================================
-- FUNCTION: Get Dashboard Summary (Multi-Tenant)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_dashboard_summary()
RETURNS TABLE (
    today_revenue NUMERIC,
    today_orders BIGINT,
    active_orders BIGINT,
    occupied_tables BIGINT,
    total_tables BIGINT,
    total_staff BIGINT
) AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'User does not have a tenant';
    END IF;
    
    RETURN QUERY
    SELECT 
        (SELECT COALESCE(SUM(total_amount), 0) 
         FROM public.orders 
         WHERE tenant_id = v_tenant_id 
         AND created_at::DATE = CURRENT_DATE
         AND status = 'paid') as today_revenue,
        
        (SELECT COUNT(*) 
         FROM public.orders 
         WHERE tenant_id = v_tenant_id 
         AND created_at::DATE = CURRENT_DATE) as today_orders,
        
        (SELECT COUNT(*) 
         FROM public.orders 
         WHERE tenant_id = v_tenant_id 
         AND status IN ('pending', 'confirmed', 'preparing')) as active_orders,
        
        (SELECT COUNT(*) 
         FROM public.tables 
         WHERE tenant_id = v_tenant_id 
         AND status = 'occupied') as occupied_tables,
        
        (SELECT COUNT(*) 
         FROM public.tables 
         WHERE tenant_id = v_tenant_id) as total_tables,
        
        (SELECT COUNT(*) 
         FROM public.profiles 
         WHERE tenant_id = v_tenant_id 
         AND is_active = true) as total_staff;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_dashboard_summary() TO authenticated;

-- ============================================================================
-- TRIGGER: Auto-update updated_at timestamp
-- ============================================================================

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all relevant tables
DROP TRIGGER IF EXISTS update_tenants_updated_at ON public.tenants;
CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON public.tenants
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_profiles_updated_at ON public.profiles;
CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_categories_updated_at ON public.categories;
CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON public.categories
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_menu_items_updated_at ON public.menu_items;
CREATE TRIGGER update_menu_items_updated_at
    BEFORE UPDATE ON public.menu_items
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_tables_updated_at ON public.tables;
CREATE TRIGGER update_tables_updated_at
    BEFORE UPDATE ON public.tables
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_orders_updated_at ON public.orders;
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON public.orders
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- List all functions
SELECT routine_name, routine_type 
FROM information_schema.routines 
WHERE routine_schema = 'public'
AND routine_name LIKE '%tenant%' OR routine_name LIKE '%staff%' OR routine_name LIKE '%dashboard%'
ORDER BY routine_name;
