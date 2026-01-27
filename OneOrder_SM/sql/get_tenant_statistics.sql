-- Function to get tenant statistics (staff, tables, menu items count)
CREATE OR REPLACE FUNCTION get_tenant_statistics()
RETURNS JSON AS $$
DECLARE
    v_tenant_id UUID;
    result JSON;
BEGIN
    -- Get current user's tenant_id
    SELECT tenant_id INTO v_tenant_id
    FROM profiles
    WHERE id = auth.uid();
    
    IF v_tenant_id IS NULL THEN
        RETURN json_build_object(
            'staff_count', 0,
            'table_count', 0,
            'menu_item_count', 0
        );
    END IF;
    
    -- Get counts
    SELECT json_build_object(
        'staff_count', (
            SELECT COUNT(*) 
            FROM profiles 
            WHERE tenant_id = v_tenant_id
        ),
        'table_count', (
            SELECT COUNT(*) 
            FROM tables 
            WHERE tenant_id = v_tenant_id
        ),
        'menu_item_count', (
            SELECT COUNT(*) 
            FROM menu_items mi
            JOIN categories c ON mi.category_id = c.id
            WHERE c.tenant_id = v_tenant_id
        )
    ) INTO result;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
