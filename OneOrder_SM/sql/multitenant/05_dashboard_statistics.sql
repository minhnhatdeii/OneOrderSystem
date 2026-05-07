-- ============================================================================
-- DASHBOARD STATISTICS TABLE & TRIGGERS
-- OneOrder_SM - Step 5: Pre-computed dashboard statistics for each tenant
-- Run this script AFTER 03_functions.sql
-- ============================================================================

-- ============================================================================
-- STEP 1: CREATE DASHBOARD_STATISTICS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.dashboard_statistics (
    tenant_id UUID PRIMARY KEY REFERENCES public.tenants(id) ON DELETE CASCADE,
    today_revenue NUMERIC DEFAULT 0,
    today_orders BIGINT DEFAULT 0,
    active_orders BIGINT DEFAULT 0,
    occupied_tables BIGINT DEFAULT 0,
    total_tables BIGINT DEFAULT 0,
    total_staff BIGINT DEFAULT 0,
    stats_date DATE DEFAULT CURRENT_DATE,
    last_updated_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS on dashboard_statistics
ALTER TABLE public.dashboard_statistics ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can only see their own tenant's statistics
DROP POLICY IF EXISTS "Users can view own tenant statistics" ON public.dashboard_statistics;
CREATE POLICY "Users can view own tenant statistics" ON public.dashboard_statistics
    FOR SELECT
    USING (tenant_id = public.get_user_tenant_id());

-- ============================================================================
-- STEP 2: CREATE FUNCTION TO REFRESH DASHBOARD STATISTICS
-- ============================================================================

CREATE OR REPLACE FUNCTION public.refresh_dashboard_statistics(p_tenant_id UUID)
RETURNS VOID AS $$
DECLARE
    v_today_revenue NUMERIC;
    v_today_orders BIGINT;
    v_active_orders BIGINT;
    v_occupied_tables BIGINT;
    v_total_tables BIGINT;
    v_total_staff BIGINT;
BEGIN
    -- Calculate today's revenue (assuming upfront payment: count all except cancelled)
    SELECT COALESCE(SUM(total_amount), 0)
    INTO v_today_revenue
    FROM public.orders
    WHERE tenant_id = p_tenant_id
    AND status != 'cancelled'
    AND (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE = (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE;
    
    -- Calculate today's total orders
    SELECT COUNT(*)
    INTO v_today_orders
    FROM public.orders
    WHERE tenant_id = p_tenant_id
    AND (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE = (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE;
    
    -- Calculate active orders (pending, confirmed, preparing)
    SELECT COUNT(*)
    INTO v_active_orders
    FROM public.orders
    WHERE tenant_id = p_tenant_id
    AND status IN ('pending', 'confirmed', 'preparing');
    
    -- Calculate occupied tables
    SELECT COUNT(*)
    INTO v_occupied_tables
    FROM public.tables
    WHERE tenant_id = p_tenant_id
    AND status = 'occupied';
    
    -- Calculate total tables
    SELECT COUNT(*)
    INTO v_total_tables
    FROM public.tables
    WHERE tenant_id = p_tenant_id;
    
    -- Calculate active staff
    SELECT COUNT(*)
    INTO v_total_staff
    FROM public.profiles
    WHERE tenant_id = p_tenant_id
    AND is_active = true;
    
    -- Upsert into dashboard_statistics
    INSERT INTO public.dashboard_statistics (
        tenant_id,
        today_revenue,
        today_orders,
        active_orders,
        occupied_tables,
        total_tables,
        total_staff,
        stats_date,
        last_updated_at
    )
    VALUES (
        p_tenant_id,
        v_today_revenue,
        v_today_orders,
        v_active_orders,
        v_occupied_tables,
        v_total_tables,
        v_total_staff,
        CURRENT_DATE,
        now()
    )
    ON CONFLICT (tenant_id) DO UPDATE SET
        today_revenue = EXCLUDED.today_revenue,
        today_orders = EXCLUDED.today_orders,
        active_orders = EXCLUDED.active_orders,
        occupied_tables = EXCLUDED.occupied_tables,
        total_tables = EXCLUDED.total_tables,
        total_staff = EXCLUDED.total_staff,
        stats_date = EXCLUDED.stats_date,
        last_updated_at = EXCLUDED.last_updated_at;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.refresh_dashboard_statistics(UUID) TO authenticated;

-- ============================================================================
-- STEP 3: CREATE TRIGGER FUNCTION FOR ORDERS
-- ============================================================================

CREATE OR REPLACE FUNCTION public.trg_fn_orders_stats()
RETURNS TRIGGER AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    -- Determine which tenant to update
    IF TG_OP = 'DELETE' THEN
        v_tenant_id := OLD.tenant_id;
    ELSE
        v_tenant_id := NEW.tenant_id;
    END IF;
    
    -- Refresh statistics for this tenant
    IF v_tenant_id IS NOT NULL THEN
        PERFORM public.refresh_dashboard_statistics(v_tenant_id);
    END IF;
    
    -- Return appropriate value
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- STEP 4: CREATE TRIGGER FUNCTION FOR TABLES
-- ============================================================================

CREATE OR REPLACE FUNCTION public.trg_fn_tables_stats()
RETURNS TRIGGER AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_tenant_id := OLD.tenant_id;
    ELSE
        v_tenant_id := NEW.tenant_id;
    END IF;
    
    IF v_tenant_id IS NOT NULL THEN
        PERFORM public.refresh_dashboard_statistics(v_tenant_id);
    END IF;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- STEP 5: CREATE TRIGGER FUNCTION FOR PROFILES (STAFF)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.trg_fn_profiles_stats()
RETURNS TRIGGER AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_tenant_id := OLD.tenant_id;
    ELSE
        v_tenant_id := NEW.tenant_id;
    END IF;
    
    IF v_tenant_id IS NOT NULL THEN
        PERFORM public.refresh_dashboard_statistics(v_tenant_id);
    END IF;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- STEP 6: CREATE TRIGGER FUNCTION FOR NEW TENANT (INITIALIZATION)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.trg_fn_init_tenant_stats()
RETURNS TRIGGER AS $$
BEGIN
    -- Initialize statistics row for new tenant
    INSERT INTO public.dashboard_statistics (
        tenant_id,
        today_revenue,
        today_orders,
        active_orders,
        occupied_tables,
        total_tables,
        total_staff,
        stats_date,
        last_updated_at
    )
    VALUES (
        NEW.id,
        0,
        0,
        0,
        0,
        0,
        0,
        CURRENT_DATE,
        now()
    )
    ON CONFLICT (tenant_id) DO NOTHING;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- STEP 7: CREATE TRIGGERS
-- ============================================================================

-- Trigger on ORDERS table
DROP TRIGGER IF EXISTS trg_orders_stats ON public.orders;
CREATE TRIGGER trg_orders_stats
    AFTER INSERT OR UPDATE OR DELETE ON public.orders
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_fn_orders_stats();

-- Trigger on TABLES table
DROP TRIGGER IF EXISTS trg_tables_stats ON public.tables;
CREATE TRIGGER trg_tables_stats
    AFTER INSERT OR UPDATE OR DELETE ON public.tables
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_fn_tables_stats();

-- Trigger on PROFILES table
DROP TRIGGER IF EXISTS trg_profiles_stats ON public.profiles;
CREATE TRIGGER trg_profiles_stats
    AFTER INSERT OR UPDATE OR DELETE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_fn_profiles_stats();

-- Trigger on TENANTS table (for initialization)
DROP TRIGGER IF EXISTS trg_tenants_init_stats ON public.tenants;
CREATE TRIGGER trg_tenants_init_stats
    AFTER INSERT ON public.tenants
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_fn_init_tenant_stats();

-- ============================================================================
-- STEP 8: UPDATE GET_DASHBOARD_SUMMARY FUNCTION
-- ============================================================================

-- DROP existing function to allow return type change
DROP FUNCTION IF EXISTS public.get_dashboard_summary();

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
    
    -- Check if stats need refresh (different date or not exists)
    IF NOT EXISTS (
        SELECT 1 FROM public.dashboard_statistics 
        WHERE tenant_id = v_tenant_id 
        AND stats_date = CURRENT_DATE
    ) THEN
        -- Refresh statistics
        PERFORM public.refresh_dashboard_statistics(v_tenant_id);
    END IF;
    
    -- Return from pre-computed table
    RETURN QUERY
    SELECT 
        ds.today_revenue,
        ds.today_orders,
        ds.active_orders,
        ds.occupied_tables,
        ds.total_tables,
        ds.total_staff
    FROM public.dashboard_statistics ds
    WHERE ds.tenant_id = v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_dashboard_summary() TO authenticated;

-- ============================================================================
-- STEP 9: INITIALIZE STATISTICS FOR EXISTING TENANTS
-- ============================================================================

DO $$
DECLARE
    t_record RECORD;
BEGIN
    FOR t_record IN SELECT id FROM public.tenants LOOP
        PERFORM public.refresh_dashboard_statistics(t_record.id);
    END LOOP;
END $$;

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Check dashboard_statistics table
SELECT * FROM public.dashboard_statistics;

-- List triggers
SELECT trigger_name, event_object_table, action_timing, event_manipulation
FROM information_schema.triggers
WHERE trigger_schema = 'public'
AND trigger_name LIKE 'trg_%stats%'
ORDER BY event_object_table;
