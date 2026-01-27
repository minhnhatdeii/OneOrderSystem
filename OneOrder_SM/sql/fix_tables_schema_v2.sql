-- Robost fix for 'tables' schema issues
-- Run this entire script in Supabase SQL Editor

-- 1. Rename 'table_number' to 'name' if 'table_number' exists
DO $$
BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='tables' AND column_name='table_number') THEN
    ALTER TABLE public.tables RENAME COLUMN table_number TO name;
  END IF;
END $$;

-- 2. Add 'capacity' column if it doesn't exist
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS capacity INTEGER NOT NULL DEFAULT 4;

-- 3. Rename 'qr_code' to 'qr_code_url' only if 'qr_code' exists
DO $$
BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='tables' AND column_name='qr_code') THEN
    ALTER TABLE public.tables RENAME COLUMN qr_code TO qr_code_url;
  END IF;
END $$;

-- 4. If 'qr_code_url' column is missing (e.g. neither exist), add it
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS qr_code_url TEXT;
