-- Query to check the tables_status_check constraint
-- Run this in Supabase SQL Editor to see what values are allowed

-- 1. View the constraint definition
SELECT 
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint
WHERE conname = 'tables_status_check';

-- 2. View current table statuses to see what's being used
SELECT DISTINCT status 
FROM tables 
ORDER BY status;

-- 3. View the full table definition
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'tables'
ORDER BY ordinal_position;
