-- Create tables and menu_items if they don't exist (Dependencies)
CREATE TABLE IF NOT EXISTS public.tables (
    id SERIAL PRIMARY KEY,
    table_number TEXT NOT NULL,
    status TEXT DEFAULT 'free', -- free, occupied
    qr_code TEXT
);

CREATE TABLE IF NOT EXISTS public.categories (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    image_url TEXT,
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.menu_items (
    id SERIAL PRIMARY KEY,
    category_id INTEGER REFERENCES public.categories(id),
    name TEXT NOT NULL,
    "desc" TEXT, -- desc is a reserved keyword in some SQL dialects, quoting just in case
    price NUMERIC(10, 2) NOT NULL,
    image_url TEXT,
    is_available BOOLEAN DEFAULT true
);

-- Create orders table
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) NOT NULL,
    table_id INTEGER REFERENCES public.tables(id),
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending', -- pending, confirmed, preparing, served, cancelled, paid
    payment_status TEXT NOT NULL DEFAULT 'unpaid', -- unpaid, paid
    idempotency_key UUID UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS public.order_items (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id UUID REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    menu_item_id INTEGER REFERENCES public.menu_items(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    price_at_time NUMERIC(10, 2) NOT NULL,
    note TEXT
);

-- Enable Row Level Security
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;

-- RLS Policies for Orders

-- Staff and Managers can view all orders
CREATE POLICY "Staff and Managers can view all orders" 
ON public.orders FOR SELECT 
USING (
  auth.uid() IN (
    SELECT id FROM public.profiles WHERE role IN ('staff', 'manager')
  )
);

-- Staff and Managers can update orders
CREATE POLICY "Staff and Managers can update orders" 
ON public.orders FOR UPDATE 
USING (
  auth.uid() IN (
    SELECT id FROM public.profiles WHERE role IN ('staff', 'manager')
  )
);

-- Customers can view their own orders
CREATE POLICY "Customers can view own orders" 
ON public.orders FOR SELECT 
USING (
  auth.uid() = user_id
);

-- Customers can insert orders (Process handled via API or Edge Function usually, but allowing insert for now)
CREATE POLICY "Customers can insert orders" 
ON public.orders FOR INSERT 
WITH CHECK (
  auth.uid() = user_id
);

-- RLS Policies for Order Items

-- Staff/Manager view all
CREATE POLICY "Staff and Managers can view all order items" 
ON public.order_items FOR SELECT 
USING (
  auth.uid() IN (
    SELECT id FROM public.profiles WHERE role IN ('staff', 'manager')
  )
);

-- Customers view own order items via order_id
CREATE POLICY "Customers can view own order items" 
ON public.order_items FOR SELECT 
USING (
  EXISTS (
    SELECT 1 FROM public.orders 
    WHERE orders.id = order_items.order_id 
    AND orders.user_id = auth.uid()
  )
);

-- Customers can insert order items
CREATE POLICY "Customers can insert order items" 
ON public.order_items FOR INSERT 
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.orders 
    WHERE orders.id = order_items.order_id 
    AND orders.user_id = auth.uid()
  )
);

-- Realtime subscriptions
-- Enable Realtime for orders table
ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;
