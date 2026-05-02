-- Migration: Thêm trạng thái 'day_off' vào bảng attendance
-- Cho phép nhân viên khai báo đơn xin nghỉ (day_off)
-- Quản lý có thể xem và duyệt/từ chối đơn này

-- Bước 1: Xóa constraint cũ
ALTER TABLE public.attendance
    DROP CONSTRAINT IF EXISTS attendance_status_check;

-- Bước 2: Thêm constraint mới với thêm giá trị 'day_off'
ALTER TABLE public.attendance
    ADD CONSTRAINT attendance_status_check
    CHECK (status IN ('pending', 'approved', 'rejected', 'day_off'));

-- Cập nhật RLS policy cho staff được insert với status day_off
-- (Không cần thay đổi vì policy đã cho phép staff insert bất kỳ status nào)

-- Verify
SELECT conname, pg_get_constraintdef(oid) AS constraint_def
FROM pg_constraint
WHERE conrelid = 'public.attendance'::regclass
  AND contype = 'c';
