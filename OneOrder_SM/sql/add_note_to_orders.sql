-- Add note column to orders table for customer notes
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS note TEXT;

-- Add comment
COMMENT ON COLUMN orders.note IS 'Customer note for the entire order (e.g., delivery instructions, special requests)';
