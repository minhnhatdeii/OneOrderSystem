-- ============================================================================
-- SIMPLIFIED TABLE STATUS TRIGGER
-- Run this in Supabase SQL Editor to replace any existing triggers
-- ============================================================================

-- Step 1: Drop any existing triggers and functions
DROP TRIGGER IF EXISTS trigger_update_table_status_on_order ON orders;
DROP TRIGGER IF EXISTS update_table_status_trigger ON orders;
DROP FUNCTION IF EXISTS update_table_status_on_order();
DROP FUNCTION IF EXISTS update_table_status();

-- Step 2: Create the trigger function
CREATE OR REPLACE FUNCTION update_table_status_on_order()
RETURNS TRIGGER AS $$
BEGIN
    -- Only process if order has a table_id
    IF NEW.table_id IS NULL THEN
        RETURN NEW;
    END IF;

    -- When order is created or status changes to active
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') AND 
       NEW.status IN ('pending', 'confirmed', 'preparing', 'served') THEN
        
        -- Mark table as occupied
        UPDATE tables
        SET status = 'occupied',
            updated_at = now()
        WHERE id = NEW.table_id
        AND tenant_id = NEW.tenant_id;
        
        RAISE NOTICE 'Table % set to OCCUPIED (order: %)', NEW.table_id, NEW.id;
    
    -- When order is completed or cancelled
    ELSIF TG_OP = 'UPDATE' AND 
          NEW.status IN ('paid', 'cancelled') AND
          OLD.status NOT IN ('paid', 'cancelled') THEN
        
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
            
            RAISE NOTICE 'Table % set to AVAILABLE (order: %)', NEW.table_id, NEW.id;
        ELSE
            RAISE NOTICE 'Table % still has active orders', NEW.table_id;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Step 3: Create the trigger
CREATE TRIGGER trigger_update_table_status_on_order
    AFTER INSERT OR UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_table_status_on_order();

-- Step 4: Verify trigger was created
SELECT 
    tgname as trigger_name,
    tgtype,
    tgenabled
FROM pg_trigger 
WHERE tgname = 'trigger_update_table_status_on_order';

-- Step 5: Test manually (optional - run after trigger is created)
-- UPDATE orders SET status = 'pending' WHERE id = 'your-order-id';
-- Then check: SELECT id, name, status FROM tables WHERE id = your-table-id;
