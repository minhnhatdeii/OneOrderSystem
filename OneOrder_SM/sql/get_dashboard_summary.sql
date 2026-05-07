-- Drop existing function if it exists
DROP FUNCTION IF EXISTS get_dashboard_summary();

-- Update dashboard summary function to include revenue from PAID orders
CREATE OR REPLACE FUNCTION get_dashboard_summary()
RETURNS JSON AS $$
DECLARE
    v_tenant_id UUID;
    result JSON;
BEGIN
    -- Get current user's tenant_id from profile
    SELECT tenant_id INTO v_tenant_id
    FROM profiles
    WHERE id = auth.uid();
    
    IF v_tenant_id IS NULL THEN
        RETURN json_build_object(
            'active_orders', 0,
            'today_revenue', 0,
            'total_tables', 0,
            'total_staff', 0
        );
    END IF;
    
    -- Build summary with revenue from PAID orders
    SELECT json_build_object(
        'active_orders', (
            SELECT COUNT(*)
            FROM orders
            WHERE tenant_id = v_tenant_id
            AND status IN ('pending', 'confirmed', 'preparing', 'served')
        ),
        'today_revenue', (
            SELECT COALESCE(SUM(total_amount), 0)
            FROM orders
            WHERE tenant_id = v_tenant_id
            AND status != 'cancelled'
            AND (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE = (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE
        ),
        'total_tables', (
            SELECT COUNT(*)
            FROM tables
            WHERE tenant_id = v_tenant_id
        ),
        'occupied_tables', (
            SELECT COUNT(*)
            FROM tables
            WHERE tenant_id = v_tenant_id
            AND status = 'occupied'
        ),
        'total_staff', (
            SELECT COUNT(*)
            FROM profiles
            WHERE tenant_id = v_tenant_id
        )
    ) INTO result;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
