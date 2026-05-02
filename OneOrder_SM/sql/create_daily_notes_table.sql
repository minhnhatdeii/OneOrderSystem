-- Bảng ghi chú ngày công cho nhân viên
-- Mỗi ngày có thể có nhiều ghi chú (ví dụ: nhiều manager note)
-- Hoặc 1 ghi chú chính (upsert theo staff_id + date)

CREATE TABLE IF NOT EXISTS public.attendance_daily_notes (
    id UUID PRIMARY KEY DEFAULT extensions.uuid_generate_v4(),
    staff_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    note_date DATE NOT NULL,
    content TEXT NOT NULL,
    created_by UUID REFERENCES public.profiles(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Mỗi nhân viên chỉ có 1 ghi chú chính mỗi ngày
    UNIQUE (staff_id, note_date)
);

-- Indexes
CREATE INDEX IF NOT EXISTS daily_notes_staff_id_idx ON public.attendance_daily_notes(staff_id);
CREATE INDEX IF NOT EXISTS daily_notes_date_idx ON public.attendance_daily_notes(note_date);
CREATE INDEX IF NOT EXISTS daily_notes_tenant_id_idx ON public.attendance_daily_notes(tenant_id);

-- Enable RLS
ALTER TABLE public.attendance_daily_notes ENABLE ROW LEVEL SECURITY;

-- Policies
-- 1. Manager có thể xem/thêm/sửa/xóa ghi chú trong tenant của mình
DROP POLICY IF EXISTS manager_all_daily_notes ON public.attendance_daily_notes;
CREATE POLICY manager_all_daily_notes ON public.attendance_daily_notes
    FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.profiles
            WHERE profiles.id = auth.uid()
            AND profiles.role = 'manager'
            AND profiles.tenant_id = attendance_daily_notes.tenant_id
        )
    );

-- 2. Staff có thể xem ghi chú của mình
DROP POLICY IF EXISTS staff_view_own_daily_notes ON public.attendance_daily_notes;
CREATE POLICY staff_view_own_daily_notes ON public.attendance_daily_notes
    FOR SELECT
    TO authenticated
    USING (staff_id = auth.uid());

-- Trigger cập nhật updated_at
DROP TRIGGER IF EXISTS update_daily_notes_updated_at ON public.attendance_daily_notes;
CREATE TRIGGER update_daily_notes_updated_at
    BEFORE UPDATE ON public.attendance_daily_notes
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();
