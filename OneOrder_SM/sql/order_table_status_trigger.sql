-- ============================================================================
-- TRIGGER: Update table status when orders are created/updated
-- Automatically marks tables as occupied/available based on order status
-- ============================================================================

-- Function: Update table status based on order changes
CREATE OR REPLACE FUNCTION update_table_status_on_order()
RETURNS TRIGGER AS $$
BEGIN
    -- When new order is created with a table
    IF TG_OP = 'INSERT' AND NEW.table_id IS NOT NULL THEN
        -- Set table to occupied if order is active
        IF NEW.status IN ('pending', 'confirmed', 'preparing', 'served') THEN
            UPDATE tables
            SET status = 'occupied',
                updated_at = now()
            WHERE id = NEW.table_id
            AND tenant_id = NEW.tenant_id;
            
            RAISE NOTICE 'Table % marked as occupied for new order %', NEW.table_id, NEW.id;
        END IF;
    
    -- When order status changes
    ELSIF TG_OP = 'UPDATE' AND NEW.table_id IS NOT NULL THEN
        -- If order completed/cancelled, check if table should be freed
        IF NEW.status IN ('paid', 'cancelled') 
           AND OLD.status NOT IN ('paid', 'cancelled') THEN
            
            -- Check if there are other active orders for this table
            IF NOT EXISTS (
                SELECT 1 FROM orders
                WHERE table_id = NEW.table_id
                AND tenant_id = NEW.tenant_id
                AND id != NEW.id
                AND status IN ('pending', 'confirmed', 'preparing', 'served')
            ) THEN
                -- No other active orders, free the table
                UPDATE tables
                SET status = 'available',
                    updated_at = now()
                WHERE id = NEW.table_id
                AND tenant_id = NEW.tenant_id;
                
                RAISE NOTICE 'Table % marked as available (order % completed)', NEW.table_id, NEW.id;
            ELSE
                RAISE NOTICE 'Table % still has active orders', NEW.table_id;
            END IF;
        -- If order status changed to active and table was available
        ELSIF NEW.status IN ('pending', 'confirmed', 'preparing', 'served')
              AND OLD.status NOT IN ('pending', 'confirmed', 'preparing', 'served') THEN
            UPDATE tables
            SET status = 'occupied',
                updated_at = now()
            WHERE id = NEW.table_id
            AND tenant_id = NEW.tenant_id;
            
            RAISE NOTICE 'Table % marked as occupied (order % reactivated)', NEW.table_id, NEW.id;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS trigger_update_table_status_on_order ON orders;

-- Create trigger on orders table
CREATE TRIGGER trigger_update_table_status_on_order
    AFTER INSERT OR UPDATE OF status, table_id ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_table_status_on_order();

-- Verification query
-- SELECT tablename, eventmanipulation, actiontiming 
-- FROM pg_trigger t 
-- JOIN pg_class c ON t.tgrelid = c.oid 
-- WHERE c.relname = 'orders' AND t.tgname = 'trigger_update_table_status_on_order';
