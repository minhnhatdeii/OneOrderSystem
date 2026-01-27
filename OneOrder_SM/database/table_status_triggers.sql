-- Auto Table Status Management for OneOrder_SM
-- This script creates database triggers to automatically update table status

-- ============================================================================
-- FUNCTION 1: Set table to BUSY when order is created
-- ============================================================================
CREATE OR REPLACE FUNCTION set_table_busy_on_order()
RETURNS TRIGGER AS $$
BEGIN
  -- Only update if table_id is provided and order is not already completed
  IF NEW.table_id IS NOT NULL AND NEW.status != 'paid' THEN
    UPDATE tables 
    SET status = 'busy'
    WHERE id = NEW.table_id;
    
    RAISE NOTICE 'Table % set to BUSY due to new order %', NEW.table_id, NEW.id;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for INSERT
CREATE TRIGGER trigger_set_table_busy_on_insert
AFTER INSERT ON orders
FOR EACH ROW
EXECUTE FUNCTION set_table_busy_on_order();

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
      
      -- If no pending orders, set table to available
      IF pending_orders_count = 0 THEN
        UPDATE tables 
        SET status = 'available'
        WHERE id = NEW.table_id;
        
        RAISE NOTICE 'Table % set to AVAILABLE - all orders completed', NEW.table_id;
      ELSE
        RAISE NOTICE 'Table % remains BUSY - % pending orders', NEW.table_id, pending_orders_count;
      END IF;
    ELSE
      -- If order status changed to something other than paid, ensure table is busy
      UPDATE tables 
      SET status = 'busy'
      WHERE id = NEW.table_id;
      
      RAISE NOTICE 'Table % set to BUSY due to order % status: %', NEW.table_id, NEW.id, NEW.status;
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
-- FUNCTION 3: Set table to available when order is deleted
-- ============================================================================
CREATE OR REPLACE FUNCTION set_table_available_on_order_delete()
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
    
    -- If no pending orders, set table to available
    IF pending_orders_count = 0 THEN
      UPDATE tables 
      SET status = 'available'
      WHERE id = OLD.table_id;
      
      RAISE NOTICE 'Table % set to AVAILABLE after order deletion', OLD.table_id;
    END IF;
  END IF;
  
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for DELETE
CREATE TRIGGER trigger_set_table_available_on_delete
AFTER DELETE ON orders
FOR EACH ROW
EXECUTE FUNCTION set_table_available_on_order_delete();

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check if triggers exist
SELECT 
  trigger_name, 
  event_manipulation, 
  event_object_table,
  action_statement
FROM information_schema.triggers
WHERE trigger_schema = 'public' 
  AND event_object_table = 'orders'
ORDER BY trigger_name;

-- Test: View current table statuses
SELECT id, name, status FROM tables ORDER BY id;

-- Test: View orders with table info
SELECT 
  o.id as order_id,
  o.table_id,
  t.name as table_name,
  t.status as table_status,
  o.status as order_status
FROM orders o
LEFT JOIN tables t ON o.table_id = t.id
ORDER BY o.created_at DESC
LIMIT 10;
