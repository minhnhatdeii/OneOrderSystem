-- ============================================================================
-- FIX: Complete Revenue Calculation & Timezone Fix
-- ============================================================================

-- STEP 1: Fix existing data
UPDATE public.orders 
SET payment_status = 'paid' 
WHERE status = 'paid' 
AND payment_status != 'paid';

-- STEP 2: Update the refresh_dashboard_statistics function
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
    
    -- Calculate active orders
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
        tenant_id, today_revenue, today_orders, active_orders,
        occupied_tables, total_tables, total_staff, stats_date, last_updated_at
    )
    VALUES (
        p_tenant_id, v_today_revenue, v_today_orders, v_active_orders,
        v_occupied_tables, v_total_tables, v_total_staff, 
        (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE, now()
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

-- STEP 3: Fix get_order_statistics to only count PAID orders for revenue
CREATE OR REPLACE FUNCTION public.get_order_statistics(
    p_start_date DATE DEFAULT (CURRENT_DATE - INTERVAL '30 days')::DATE,
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
        (o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE as order_date,
        COUNT(*)::BIGINT as total_orders,
        -- Sum revenue for all orders except cancelled (upfront payment logic)
        COALESCE(SUM(o.total_amount) FILTER (WHERE o.status != 'cancelled'), 0) as total_revenue,
        COUNT(*) FILTER (WHERE o.status = 'pending')::BIGINT as pending_orders,
        COUNT(*) FILTER (WHERE o.status IN ('served', 'paid'))::BIGINT as completed_orders,
        COUNT(*) FILTER (WHERE o.status = 'cancelled')::BIGINT as cancelled_orders
    FROM public.orders o
    WHERE o.tenant_id = v_tenant_id
    AND (o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE BETWEEN p_start_date AND p_end_date
    GROUP BY (o.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE
    ORDER BY order_date DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- STEP 4: Refresh all tenant statistics
DO $$
DECLARE
    t_record RECORD;
BEGIN
    FOR t_record IN SELECT id FROM public.tenants LOOP
        PERFORM public.refresh_dashboard_statistics(t_record.id);
    END LOOP;
END $$;

-- STEP 5: Rewrite get_dashboard_summary to return JSON (so decodeSingle in app works)
DROP FUNCTION IF EXISTS public.get_dashboard_summary();

CREATE OR REPLACE FUNCTION public.get_dashboard_summary()
RETURNS JSON AS $$
DECLARE
    v_tenant_id UUID;
    result JSON;
BEGIN
    v_tenant_id := public.get_user_tenant_id();
    
    IF v_tenant_id IS NULL THEN
        RETURN json_build_object(
            'today_revenue', 0,
            'today_orders', 0,
            'active_orders', 0,
            'occupied_tables', 0,
            'total_tables', 0,
            'total_staff', 0
        );
    END IF;
    
    -- Check if stats need refresh
    IF NOT EXISTS (
        SELECT 1 FROM public.dashboard_statistics 
        WHERE tenant_id = v_tenant_id 
        AND stats_date = (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE
    ) THEN
        PERFORM public.refresh_dashboard_statistics(v_tenant_id);
    END IF;
    
    -- Build JSON from dashboard_statistics table
    SELECT json_build_object(
        'today_revenue', COALESCE(ds.today_revenue, 0),
        'today_orders', COALESCE(ds.today_orders, 0),
        'active_orders', COALESCE(ds.active_orders, 0),
        'occupied_tables', COALESCE(ds.occupied_tables, 0),
        'total_tables', COALESCE(ds.total_tables, 0),
        'total_staff', COALESCE(ds.total_staff, 0)
    )
    INTO result
    FROM public.dashboard_statistics ds
    WHERE ds.tenant_id = v_tenant_id;
    
    IF result IS NULL THEN
        RETURN json_build_object(
            'today_revenue', 0,
            'today_orders', 0,
            'active_orders', 0,
            'occupied_tables', 0,
            'total_tables', 0,
            'total_staff', 0
        );
    END IF;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.get_dashboard_summary() TO authenticated;
