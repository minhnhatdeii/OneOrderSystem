-- Updated test orders script with notes
DO $$
DECLARE
    v_tenant_id UUID;
    v_table_1 BIGINT;
    v_menu_item_1 BIGINT;
    v_order_1 UUID;  -- Changed from BIGINT to UUID
    v_order_2 UUID;  -- Changed from BIGINT to UUID
    v_order_3 UUID;  -- Changed from BIGINT to UUID
BEGIN
    SELECT id INTO v_tenant_id FROM tenants ORDER BY created_at DESC LIMIT 1;
    
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'No tenants found';
    END IF;
    
    SELECT id INTO v_table_1 FROM tables WHERE tenant_id = v_tenant_id LIMIT 1;
    
    SELECT mi.id INTO v_menu_item_1
    FROM menu_items mi
    JOIN categories c ON mi.category_id = c.id
    WHERE c.tenant_id = v_tenant_id
    LIMIT 1;
    
    IF v_table_1 IS NULL OR v_menu_item_1 IS NULL THEN
        RAISE NOTICE 'Please create tables and menu items first!';
        RETURN;
    END IF;
    
    -- Order 1: PENDING with customer note
    INSERT INTO orders (table_id, status, total_amount, payment_status, note, tenant_id, user_id)
    SELECT v_table_1, 'pending', 150000, 'unpaid', 'Không hành, ít cay', v_tenant_id, id
    FROM profiles WHERE tenant_id = v_tenant_id LIMIT 1
    RETURNING id INTO v_order_1;
    
    INSERT INTO order_items (order_id, menu_item_id, quantity, price_at_time)
    VALUES (v_order_1, v_menu_item_1, 2, 75000);
    
    -- Order 2: PREPARING
    INSERT INTO orders (table_id, status, total_amount, payment_status, note, tenant_id, user_id, created_at)
    SELECT v_table_1, 'preparing', 200000, 'unpaid', 'Giao nhanh', v_tenant_id, id, NOW() - INTERVAL '20 minutes'
    FROM profiles WHERE tenant_id = v_tenant_id LIMIT 1
    RETURNING id INTO v_order_2;
    
    INSERT INTO order_items (order_id, menu_item_id, quantity, price_at_time, note)
    VALUES (v_order_2, v_menu_item_1, 3, 65000, 'Thêm nước tương');
    
    -- Order 3: SERVED
    INSERT INTO orders (table_id, status, total_amount, payment_status, tenant_id, user_id, created_at)
    SELECT v_table_1, 'served', 250000, 'unpaid', v_tenant_id, id, NOW() - INTERVAL '1 hour'
    FROM profiles WHERE tenant_id = v_tenant_id LIMIT 1
    RETURNING id INTO v_order_3;
    
    INSERT INTO order_items (order_id, menu_item_id, quantity, price_at_time)
    VALUES (v_order_3, v_menu_item_1, 4, 62500);
    
    RAISE NOTICE 'Created 3 test orders with notes!';
END $$;
