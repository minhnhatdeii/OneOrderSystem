-- ============================================================================
-- CREATE VIEW: orders_with_details
-- Returns orders with table names and menu item names pre-joined
-- ============================================================================

CREATE OR REPLACE VIEW orders_with_details AS
SELECT 
    o.id,
    o.tenant_id,
    o.user_id,
    o.table_id,
    t.name as table_name,  -- Joined from tables
    o.total_amount,
    o.status,
    o.payment_status,
    o.note,
    o.created_at,
    o.updated_at,
    
    -- Aggregate order items as JSON array with item names
    COALESCE(
        json_agg(
            json_build_object(
                'id', oi.id,
                'order_id', oi.order_id,
                'menu_item_id', oi.menu_item_id,
                'item_name', mi.name,  -- Joined from menu_items
                'quantity', oi.quantity,
                'price_at_time', oi.price_at_time,
                'note', oi.note
            ) ORDER BY oi.created_at
        ) FILTER (WHERE oi.id IS NOT NULL),
        '[]'::json
    ) as order_items
    
FROM orders o
LEFT JOIN tables t ON t.id = o.table_id AND t.tenant_id = o.tenant_id
LEFT JOIN order_items oi ON oi.order_id = o.id
LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id AND mi.tenant_id = o.tenant_id

GROUP BY 
    o.id, 
    o.tenant_id,
    o.user_id,
    o.table_id,
    t.name,
    o.total_amount,
    o.status,
    o.payment_status,
    o.note,
    o.created_at,
    o.updated_at;

-- Grant select permission to authenticated users
GRANT SELECT ON orders_with_details TO authenticated;

-- Add RLS policy for the view
ALTER VIEW orders_with_details SET (security_invoker = true);
