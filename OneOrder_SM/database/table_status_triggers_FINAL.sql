-- FINAL: Auto Table Status Management for OneOrder_SM
-- Correct status values: 'occupied' and 'free'

-- DROP existing triggers if any (all possible names)
DROP TRIGGER IF EXISTS trigger_set_table_busy_on_insert ON orders;
DROP TRIGGER IF EXISTS trigger_set_table_occupied_on_insert ON orders;
DROP TRIGGER IF EXISTS trigger_update_table_status_on_order_update ON orders;
DROP TRIGGER IF EXISTS trigger_set_table_available_on_delete ON orders;
DROP TRIGGER IF EXISTS trigger_set_table_free_on_delete ON orders;
DROP TRIGGER IF EXISTS trigger_update_table_status_on_order ON orders;

DROP FUNCTION IF EXISTS set_table_busy_on_order();
DROP FUNCTION IF EXISTS set_table_occupied_on_order();
DROP FUNCTION IF EXISTS update_table_status_on_order_update();
DROP FUNCTION IF EXISTS set_table_available_on_order_delete();
DROP FUNCTION IF EXISTS set_table_free_on_order_delete();
DROP FUNCTION IF EXISTS update_table_status_on_order();

-- ============================================================================
-- FUNCTION 1: Set table to OCCUPIED when order is created
-- ============================================================================
CREATE OR REPLACE FUNCTION set_table_occupied_on_order()
RETURNS TRIGGER AS $$
BEGIN
  -- Only update if table_id is provided and order is not already completed
  IF NEW.table_id IS NOT NULL AND NEW.status != 'paid' THEN
    UPDATE tables 
    SET status = 'occupied'
    WHERE id = NEW.table_id;
    
    RAISE NOTICE 'Table % set to OCCUPIED due to new order %', NEW.table_id, NEW.id;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for INSERT
CREATE TRIGGER trigger_set_table_occupied_on_insert
AFTER INSERT ON orders
FOR EACH ROW
EXECUTE FUNCTION set_table_occupied_on_order();

-- ============================================================================
-- FUNCTION 2: Update table status when order status changes
-- ============================================================================
CREATE OR REPLACE FUNCTION update_table_status_on_order_update()
RETURNS TRIGGER AS $$
DECLARE
  pending_orders_count INTEGER;
BEGIN
  -- Only process if table_id exists and status actually changed
  IF NEW.table_id IS NOT NULL AND OLD.status != NEW.status THEN
    
    -- Check if the order is now completed (paid status)
    IF NEW.status = 'paid' THEN
      -- Count remaining non-completed orders for this table
      SELECT COUNT(*) INTO pending_orders_count
      FROM orders
      WHERE table_id = NEW.table_id 
        AND status != 'paid'
        AND id != NEW.id; -- Exclude current order
      
      -- If no pending orders, set table to free
      IF pending_orders_count = 0 THEN
        UPDATE tables 
        SET status = 'free'
        WHERE id = NEW.table_id;
        
        RAISE NOTICE 'Table % set to FREE - all orders completed', NEW.table_id;
      ELSE
        RAISE NOTICE 'Table % remains OCCUPIED - % pending orders', NEW.table_id, pending_orders_count;
      END IF;
    ELSE
      -- If order status changed to something other than paid, ensure table is occupied
      UPDATE tables 
      SET status = 'occupied'
      WHERE id = NEW.table_id;
      
      RAISE NOTICE 'Table % set to OCCUPIED due to order % status: %', NEW.table_id, NEW.id, NEW.status;
    END IF;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for UPDATE
CREATE TRIGGER trigger_update_table_status_on_order_update
AFTER UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION update_table_status_on_order_update();

-- ============================================================================
-- FUNCTION 3: Set table to free when order is deleted
-- ============================================================================
CREATE OR REPLACE FUNCTION set_table_free_on_order_delete()
RETURNS TRIGGER AS $$
DECLARE
  pending_orders_count INTEGER;
BEGIN
  IF OLD.table_id IS NOT NULL THEN
    -- Count remaining orders for this table
    SELECT COUNT(*) INTO pending_orders_count
    FROM orders
    WHERE table_id = OLD.table_id 
      AND status != 'paid';
    
    -- If no pending orders, set table to free
    IF pending_orders_count = 0 THEN
      UPDATE tables 
      SET status = 'free'
      WHERE id = OLD.table_id;
      
      RAISE NOTICE 'Table % set to FREE after order deletion', OLD.table_id;
    END IF;
  END IF;
  
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for DELETE
CREATE TRIGGER trigger_set_table_free_on_delete
AFTER DELETE ON orders
FOR EACH ROW
EXECUTE FUNCTION set_table_free_on_order_delete();

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Check if triggers were created successfully
SELECT 
  trigger_name, 
  event_manipulation, 
  event_object_table,
  action_statement
FROM information_schema.triggers
WHERE trigger_schema = 'public' 
  AND event_object_table = 'orders'
  AND trigger_name LIKE '%table%'
ORDER BY trigger_name;

-- View current table statuses
SELECT id, name, status, capacity 
FROM tables 
ORDER BY id;

-- View recent orders with table info
SELECT 
  o.id as order_id,
  o.table_id,
  t.name as table_name,
  t.status as table_status,
  o.status as order_status,
  o.created_at
FROM orders o
LEFT JOIN tables t ON o.table_id = t.id
ORDER BY o.created_at DESC
LIMIT 10;

-- ============================================================================
-- SYNC: Update existing table statuses based on current orders
-- Run this ONCE to fix tables that have active orders but wrong status
-- ============================================================================

-- Step 1: Set ALL tables with active orders to OCCUPIED
UPDATE tables t
SET status = 'occupied'
WHERE EXISTS (
    SELECT 1 FROM orders o
    WHERE o.table_id = t.id
    AND o.status IN ('pending', 'confirmed', 'preparing', 'served')
);

-- Step 2: Set tables with NO active orders to FREE
UPDATE tables t
SET status = 'free'
WHERE NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.table_id = t.id
    AND o.status IN ('pending', 'confirmed', 'preparing', 'served')
);

-- Verify results
SELECT 
  t.id, 
  t.name, 
  t.status,
  (SELECT COUNT(*) FROM orders o WHERE o.table_id = t.id AND o.status IN ('pending', 'confirmed', 'preparing', 'served')) as active_orders
FROM tables t
ORDER BY t.id;
