-- Add location column to tables
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS location TEXT;

-- Update existing tables with default location (optional)
-- UPDATE public.tables SET location = 'Tầng 1' WHERE location IS NULL;
