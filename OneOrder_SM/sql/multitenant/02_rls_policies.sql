-- ============================================================================
-- MULTI-TENANT RLS POLICIES
-- OneOrder_SM - Step 2: Create Row Level Security Policies for Multi-Tenant
-- Run this script AFTER 01_create_schema.sql
-- ============================================================================

-- ============================================================================
-- DROP EXISTING POLICIES (to recreate with tenant awareness)
-- ============================================================================

-- Tenants policies (new)
-- (No existing policies to drop)

-- Profiles policies
DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;

-- Categories policies
DROP POLICY IF EXISTS "Managers full access to categories" ON public.categories;
DROP POLICY IF EXISTS "Staff can view categories" ON public.categories;
DROP POLICY IF EXISTS "Customers can view active categories" ON public.categories;

-- Menu items policies
DROP POLICY IF EXISTS "Managers full access to menu_items" ON public.menu_items;
DROP POLICY IF EXISTS "Staff can view menu_items" ON public.menu_items;
DROP POLICY IF EXISTS "Customers can view available menu_items" ON public.menu_items;

-- Tables policies
DROP POLICY IF EXISTS "Managers full access to tables" ON public.tables;
DROP POLICY IF EXISTS "Staff can view tables" ON public.tables;
DROP POLICY IF EXISTS "Staff can update table status" ON public.tables;
DROP POLICY IF EXISTS "Customers can view tables" ON public.tables;

-- Orders policies
DROP POLICY IF EXISTS "Staff and Managers can view all orders" ON public.orders;
DROP POLICY IF EXISTS "Staff and Managers can update orders" ON public.orders;
DROP POLICY IF EXISTS "Customers can view own orders" ON public.orders;
DROP POLICY IF EXISTS "Customers can insert orders" ON public.orders;

-- Order items policies
DROP POLICY IF EXISTS "Staff and Managers can view all order items" ON public.order_items;
DROP POLICY IF EXISTS "Customers can view own order items" ON public.order_items;
DROP POLICY IF EXISTS "Customers can insert order items" ON public.order_items;

-- ============================================================================
-- RLS POLICIES FOR TENANTS TABLE
-- ============================================================================

-- Managers can view their own tenant
CREATE POLICY "Managers can view own tenant"
ON public.tenants FOR SELECT
USING (
    id = public.get_user_tenant_id()
);

-- Managers can update their own tenant
CREATE POLICY "Managers can update own tenant"
ON public.tenants FOR UPDATE
USING (
    owner_id = auth.uid()
);

-- Users can create tenants (during registration)
CREATE POLICY "Users can create tenants"
ON public.tenants FOR INSERT
WITH CHECK (
    owner_id = auth.uid()
);

-- ============================================================================
-- RLS POLICIES FOR PROFILES TABLE
-- ============================================================================

-- Users can view their own profile
CREATE POLICY "Users can view own profile"
ON public.profiles FOR SELECT
USING (id = auth.uid());

-- Managers can view all staff in their tenant
CREATE POLICY "Managers can view tenant staff"
ON public.profiles FOR SELECT
USING (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
);

-- Users can update their own profile
CREATE POLICY "Users can update own profile"
ON public.profiles FOR UPDATE
USING (id = auth.uid());

-- Managers can update staff in their tenant
CREATE POLICY "Managers can update tenant staff"
ON public.profiles FOR UPDATE
USING (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id() AND
    id != auth.uid() -- Cannot demote yourself
);

-- Managers can insert new staff (via Edge Function with service role)
-- Direct insert is restricted, use Edge Function instead

-- ============================================================================
-- RLS POLICIES FOR CATEGORIES TABLE
-- ============================================================================

-- Managers can do everything with their tenant's categories
CREATE POLICY "Managers full access to tenant categories"
ON public.categories FOR ALL
USING (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
)
WITH CHECK (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
);

-- Staff can view their tenant's categories
CREATE POLICY "Staff can view tenant categories"
ON public.categories FOR SELECT
USING (
    public.get_user_role() = 'staff' AND
    tenant_id = public.get_user_tenant_id()
);

-- Customers can view active categories from any tenant (for menu browsing)
CREATE POLICY "Customers can view active categories"
ON public.categories FOR SELECT
USING (
    is_active = true
);

-- ============================================================================
-- RLS POLICIES FOR MENU_ITEMS TABLE
-- ============================================================================

-- Managers can do everything with their tenant's menu items
CREATE POLICY "Managers full access to tenant menu_items"
ON public.menu_items FOR ALL
USING (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
)
WITH CHECK (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
);

-- Staff can view their tenant's menu items
CREATE POLICY "Staff can view tenant menu_items"
ON public.menu_items FOR SELECT
USING (
    public.get_user_role() = 'staff' AND
    tenant_id = public.get_user_tenant_id()
);

-- Customers can view available menu items from any tenant
CREATE POLICY "Customers can view available menu_items"
ON public.menu_items FOR SELECT
USING (
    is_available = true
);

-- ============================================================================
-- RLS POLICIES FOR TABLES TABLE
-- ============================================================================

-- Managers can do everything with their tenant's tables
CREATE POLICY "Managers full access to tenant tables"
ON public.tables FOR ALL
USING (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
)
WITH CHECK (
    public.get_user_role() = 'manager' AND
    tenant_id = public.get_user_tenant_id()
);

-- Staff can view their tenant's tables
CREATE POLICY "Staff can view tenant tables"
ON public.tables FOR SELECT
USING (
    public.get_user_role() = 'staff' AND
    tenant_id = public.get_user_tenant_id()
);

-- Staff can update table status in their tenant
CREATE POLICY "Staff can update tenant table status"
ON public.tables FOR UPDATE
USING (
    public.get_user_role() = 'staff' AND
    tenant_id = public.get_user_tenant_id()
)
WITH CHECK (
    public.get_user_role() = 'staff' AND
    tenant_id = public.get_user_tenant_id()
);

-- Customers can view tables from any tenant (for QR code scanning)
CREATE POLICY "Customers can view tables"
ON public.tables FOR SELECT
USING (true);

-- ============================================================================
-- RLS POLICIES FOR ORDERS TABLE
-- ============================================================================

-- Staff and Managers can view orders in their tenant
CREATE POLICY "Staff and Managers can view tenant orders"
ON public.orders FOR SELECT
USING (
    public.get_user_role() IN ('staff', 'manager') AND
    tenant_id = public.get_user_tenant_id()
);

-- Staff and Managers can update orders in their tenant
CREATE POLICY "Staff and Managers can update tenant orders"
ON public.orders FOR UPDATE
USING (
    public.get_user_role() IN ('staff', 'manager') AND
    tenant_id = public.get_user_tenant_id()
);

-- Customers can view their own orders
CREATE POLICY "Customers can view own orders"
ON public.orders FOR SELECT
USING (
    user_id = auth.uid()
);

-- Customers can create orders (tenant_id comes from the table they're ordering from)
CREATE POLICY "Customers can insert orders"
ON public.orders FOR INSERT
WITH CHECK (
    user_id = auth.uid()
);

-- ============================================================================
-- RLS POLICIES FOR ORDER_ITEMS TABLE
-- ============================================================================

-- Staff and Managers can view order items in their tenant
CREATE POLICY "Staff and Managers can view tenant order items"
ON public.order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.orders o
        WHERE o.id = order_items.order_id
        AND o.tenant_id = public.get_user_tenant_id()
        AND public.get_user_role() IN ('staff', 'manager')
    )
);

-- Customers can view their own order items
CREATE POLICY "Customers can view own order items"
ON public.order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.orders
        WHERE orders.id = order_items.order_id
        AND orders.user_id = auth.uid()
    )
);

-- Customers can insert order items for their orders
CREATE POLICY "Customers can insert order items"
ON public.order_items FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.orders
        WHERE orders.id = order_items.order_id
        AND orders.user_id = auth.uid()
    )
);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check all policies are created
SELECT schemaname, tablename, policyname 
FROM pg_policies 
WHERE schemaname = 'public' 
ORDER BY tablename, policyname;
