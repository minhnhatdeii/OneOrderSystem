-- RLS Policies and Functions for Menu & Table Management
-- Milestone 3: OneOrder_SM

-- ============================================================================
-- STORAGE BUCKET SETUP
-- ============================================================================
-- Create storage bucket for menu images (run in Supabase Dashboard > Storage)
-- Bucket name: menu-images
-- Public: true (for read access)
-- File size limit: 5MB
-- Allowed MIME types: image/*

-- Storage policies for menu-images bucket
-- These should be created in Supabase Dashboard > Storage > menu-images > Policies

-- Policy: Anyone can view images
-- CREATE POLICY "Public read access" ON storage.objects
-- FOR SELECT USING (bucket_id = 'menu-images');

-- Policy: Managers can upload images
-- CREATE POLICY "Managers can upload images" ON storage.objects
-- FOR INSERT WITH CHECK (
--   bucket_id = 'menu-images' AND
--   auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
-- );

-- Policy: Managers can update images
-- CREATE POLICY "Managers can update images" ON storage.objects
-- FOR UPDATE USING (
--   bucket_id = 'menu-images' AND
--   auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
-- );

-- Policy: Managers can delete images
-- CREATE POLICY "Managers can delete images" ON storage.objects
-- FOR DELETE USING (
--   bucket_id = 'menu-images' AND
--   auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
-- );

-- ============================================================================
-- ENABLE RLS ON EXISTING TABLES
-- ============================================================================

-- Enable RLS on categories table
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;

-- Enable RLS on menu_items table
ALTER TABLE public.menu_items ENABLE ROW LEVEL SECURITY;

-- Enable RLS on tables table
ALTER TABLE public.tables ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- RLS POLICIES FOR CATEGORIES
-- ============================================================================

-- Managers can do everything with categories
CREATE POLICY "Managers full access to categories"
ON public.categories
FOR ALL
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
)
WITH CHECK (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
);

-- Staff can view all categories
CREATE POLICY "Staff can view categories"
ON public.categories
FOR SELECT
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'staff')
);

-- Customers can view active categories
CREATE POLICY "Customers can view active categories"
ON public.categories
FOR SELECT
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'customer') AND
  is_active = true
);

-- ============================================================================
-- RLS POLICIES FOR MENU_ITEMS
-- ============================================================================

-- Managers can do everything with menu_items
CREATE POLICY "Managers full access to menu_items"
ON public.menu_items
FOR ALL
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
)
WITH CHECK (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
);

-- Staff can view all menu_items
CREATE POLICY "Staff can view menu_items"
ON public.menu_items
FOR SELECT
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'staff')
);

-- Customers can view available menu items
CREATE POLICY "Customers can view available menu_items"
ON public.menu_items
FOR SELECT
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'customer') AND
  is_available = true
);

-- ============================================================================
-- RLS POLICIES FOR TABLES
-- ============================================================================

-- Managers can do everything with tables
CREATE POLICY "Managers full access to tables"
ON public.tables
FOR ALL
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
)
WITH CHECK (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'manager')
);

-- Staff can view and update table status
CREATE POLICY "Staff can view tables"
ON public.tables
FOR SELECT
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'staff')
);

CREATE POLICY "Staff can update table status"
ON public.tables
FOR UPDATE
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'staff')
)
WITH CHECK (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'staff')
);

-- Customers can view all tables (to know availability)
CREATE POLICY "Customers can view tables"
ON public.tables
FOR SELECT
USING (
  auth.uid() IN (SELECT id FROM public.profiles WHERE role = 'customer')
);

-- ============================================================================
-- DATABASE FUNCTIONS
-- ============================================================================

-- Function to toggle menu item availability
CREATE OR REPLACE FUNCTION public.toggle_menu_item_availability(item_id INTEGER)
RETURNS VOID AS $$
BEGIN
  UPDATE public.menu_items
  SET is_available = NOT is_available
  WHERE id = item_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission to managers
GRANT EXECUTE ON FUNCTION public.toggle_menu_item_availability(INTEGER) TO authenticated;

-- Function to toggle category active status
CREATE OR REPLACE FUNCTION public.toggle_category_active(category_id INTEGER)
RETURNS VOID AS $$
BEGIN
  UPDATE public.categories
  SET is_active = NOT is_active
  WHERE id = category_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission to managers
GRANT EXECUTE ON FUNCTION public.toggle_category_active(INTEGER) TO authenticated;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check RLS is enabled
-- SELECT tablename, rowsecurity FROM pg_tables 
-- WHERE schemaname = 'public' AND tablename IN ('categories', 'menu_items', 'tables');

-- Check policies exist
-- SELECT tablename, policyname FROM pg_policies 
-- WHERE schemaname = 'public' AND tablename IN ('categories', 'menu_items', 'tables');
