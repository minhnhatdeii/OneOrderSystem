-- Table to store attendance logs
CREATE TABLE IF NOT EXISTS public.attendance (
    id UUID PRIMARY KEY DEFAULT extensions.uuid_generate_v4(),
    staff_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL DEFAULT CURRENT_DATE,
    check_in TIMESTAMPTZ NOT NULL,
    check_out TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'day_off')),
    total_hours NUMERIC(5, 2) DEFAULT 0,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexing for performance
CREATE INDEX IF NOT EXISTS attendance_staff_id_idx ON public.attendance(staff_id);
CREATE INDEX IF NOT EXISTS attendance_date_idx ON public.attendance(attendance_date);
CREATE INDEX IF NOT EXISTS attendance_tenant_id_idx ON public.attendance(tenant_id);

-- Enable RLS
ALTER TABLE public.attendance ENABLE ROW LEVEL SECURITY;

-- Policies
-- 1. Managers can do everything within their tenant
DROP POLICY IF EXISTS manager_all_attendance ON public.attendance;
CREATE POLICY manager_all_attendance ON public.attendance
    FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.profiles
            WHERE profiles.id = auth.uid()
            AND profiles.role = 'manager'
            AND profiles.tenant_id = attendance.tenant_id
        )
    );

-- 2. Staff can view their own attendance
DROP POLICY IF EXISTS staff_view_own_attendance ON public.attendance;
CREATE POLICY staff_view_own_attendance ON public.attendance
    FOR SELECT
    TO authenticated
    USING (staff_id = auth.uid());

-- 3. Staff can insert their own attendance (check-in)
DROP POLICY IF EXISTS staff_insert_own_attendance ON public.attendance;
CREATE POLICY staff_insert_own_attendance ON public.attendance
    FOR INSERT
    TO authenticated
    WITH CHECK (staff_id = auth.uid());

-- 4. Staff can update their own attendance (check-out)
-- Note: Restriction to "current day" and "not approved" should be handled in app logic or trigger,
-- but for RLS we allow updating IF it's their own record.
DROP POLICY IF EXISTS staff_update_own_attendance ON public.attendance;
CREATE POLICY staff_update_own_attendance ON public.attendance
    FOR UPDATE
    TO authenticated
    USING (staff_id = auth.uid() AND status = 'pending')
    WITH CHECK (staff_id = auth.uid() AND status = 'pending');

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_attendance_updated_at ON public.attendance;
CREATE TRIGGER update_attendance_updated_at
    BEFORE UPDATE ON public.attendance
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- Function to calculate total hours before update/insert
CREATE OR REPLACE FUNCTION calculate_attendance_hours()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.check_in IS NOT NULL AND NEW.check_out IS NOT NULL THEN
        NEW.total_hours := EXTRACT(EPOCH FROM (NEW.check_out - NEW.check_in)) / 3600;
    ELSE
        NEW.total_hours := 0;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS trigger_calculate_hours ON public.attendance;
CREATE TRIGGER trigger_calculate_hours
    BEFORE INSERT OR UPDATE ON public.attendance
    FOR EACH ROW
    EXECUTE PROCEDURE calculate_attendance_hours();
