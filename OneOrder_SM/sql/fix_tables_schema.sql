-- Fix 'tables' table schema to match Android app 'Table' model

-- 1. Rename 'table_number' to 'name'
ALTER TABLE public.tables RENAME COLUMN table_number TO name;

-- 2. Add 'capacity' column (default to 4 if not specified)
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS capacity INTEGER NOT NULL DEFAULT 4;

-- 3. Rename 'qr_code' to 'qr_code_url' to match JSON property
ALTER TABLE public.tables RENAME COLUMN qr_code TO qr_code_url;

-- 4. Verify the changes (Optional)
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'tables';
