-- ============================================================================
-- MULTI-TENANT ARCHITECTURE MIGRATION
-- OneOrder_SM - Step 1: Create tenants table and update profiles
-- Run this script in Supabase SQL Editor
-- ============================================================================

-- ============================================================================
-- STEP 1: CREATE TENANTS TABLE
-- ============================================================================

-- Create the tenants table (represents each restaurant)
CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES auth.users(id),
    restaurant_name TEXT NOT NULL,
    business_type TEXT DEFAULT 'restaurant', -- restaurant, cafe, bar, etc.
    address TEXT,
    phone_number TEXT,
    email TEXT,
    logo_url TEXT,
    timezone TEXT DEFAULT 'Asia/Ho_Chi_Minh',
    currency TEXT DEFAULT 'VND',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Create indexes for tenants
CREATE INDEX IF NOT EXISTS idx_tenants_owner_id ON public.tenants(owner_id);
CREATE INDEX IF NOT EXISTS idx_tenants_is_active ON public.tenants(is_active);

-- Enable RLS on tenants
ALTER TABLE public.tenants ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- STEP 2: UPDATE PROFILES TABLE
-- ============================================================================

-- Add tenant_id column to profiles (nullable initially for migration)
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE;

-- Add created_by column (who created this account)
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users(id);

-- Add is_active column for soft delete
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Add updated_at column
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

-- Create indexes for profiles
CREATE INDEX IF NOT EXISTS idx_profiles_tenant_id ON public.profiles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);
CREATE INDEX IF NOT EXISTS idx_profiles_is_active ON public.profiles(is_active);
CREATE INDEX IF NOT EXISTS idx_profiles_created_by ON public.profiles(created_by);

-- ============================================================================
-- STEP 3: UPDATE CATEGORIES TABLE
-- ============================================================================

-- Add tenant_id column
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE;

-- Add created_by column
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users(id);

-- Add updated_at column
ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_categories_tenant_id ON public.categories(tenant_id);
CREATE INDEX IF NOT EXISTS idx_categories_tenant_active ON public.categories(tenant_id, is_active);

-- ============================================================================
-- STEP 4: UPDATE MENU_ITEMS TABLE
-- ============================================================================

-- Add tenant_id column
ALTER TABLE public.menu_items ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE;

-- Add created_by column
ALTER TABLE public.menu_items ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users(id);

-- Add updated_at column
ALTER TABLE public.menu_items ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_menu_items_tenant_id ON public.menu_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_tenant_category ON public.menu_items(tenant_id, category_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_tenant_available ON public.menu_items(tenant_id, is_available);

-- ============================================================================
-- STEP 5: UPDATE TABLES TABLE
-- ============================================================================

-- Add tenant_id column
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE;

-- Add created_by column
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users(id);

-- Add created_at column
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();

-- Add updated_at column
ALTER TABLE public.tables ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_tables_tenant_id ON public.tables(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tables_tenant_status ON public.tables(tenant_id, status);

-- ============================================================================
-- STEP 6: UPDATE ORDERS TABLE
-- ============================================================================

-- Add tenant_id column
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id ON public.orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_status ON public.orders(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_created ON public.orders(tenant_id, created_at DESC);

-- ============================================================================
-- STEP 7: CREATE HELPER FUNCTION TO GET CURRENT USER'S TENANT
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_user_tenant_id()
RETURNS UUID AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    SELECT tenant_id INTO v_tenant_id
    FROM public.profiles
    WHERE id = auth.uid();
    
    RETURN v_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.get_user_tenant_id() TO authenticated;

-- ============================================================================
-- STEP 8: CREATE HELPER FUNCTION TO GET CURRENT USER'S ROLE
-- ============================================================================

CREATE OR REPLACE FUNCTION public.get_user_role()
RETURNS TEXT AS $$
DECLARE
    v_role TEXT;
BEGIN
    SELECT role INTO v_role
    FROM public.profiles
    WHERE id = auth.uid();
    
    RETURN v_role;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.get_user_role() TO authenticated;

-- ============================================================================
-- STEP 9: CREATE FUNCTION TO CHECK IF USER IS MANAGER OF TENANT
-- ============================================================================

CREATE OR REPLACE FUNCTION public.is_tenant_manager(p_tenant_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_role TEXT;
    v_user_tenant_id UUID;
BEGIN
    SELECT role, tenant_id INTO v_role, v_user_tenant_id
    FROM public.profiles
    WHERE id = auth.uid();
    
    RETURN v_role = 'manager' AND v_user_tenant_id = p_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

GRANT EXECUTE ON FUNCTION public.is_tenant_manager(UUID) TO authenticated;

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Check that all tables have tenant_id column
SELECT table_name, column_name 
FROM information_schema.columns 
WHERE table_schema = 'public' 
AND column_name = 'tenant_id'
ORDER BY table_name;
