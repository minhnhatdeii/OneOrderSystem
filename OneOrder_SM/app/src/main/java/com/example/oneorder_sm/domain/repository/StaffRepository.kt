package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.domain.model.Attendance
import com.example.oneorder_sm.domain.model.AttendanceStatus
import com.example.oneorder_sm.domain.model.DailyNote
import com.example.oneorder_sm.domain.model.Profile

/**
 * Repository interface for staff management operations.
 * Direct account creation - no invitation flow.
 */
interface StaffRepository {
    
    /**
     * Gets list of all staff members in the current tenant.
     */
    suspend fun getStaffList(): Result<List<Profile>>
    
    suspend fun getCurrentStaffProfile(): Result<Profile>
    
    /**
     * Creates a staff account directly.
     * @return The temporary password (123456)
     */
    suspend fun createStaffAccount(
        email: String,
        fullName: String,
        phone: String?,
        role: String // "staff" or "manager"
    ): Result<String>
    
    /**
     * Deactivates a staff account (soft delete).
     */
    suspend fun deactivateStaff(staffId: String): Result<Unit>
    
    /**
     * Reactivates a previously deactivated staff account.
     */
    suspend fun reactivateStaff(staffId: String): Result<Unit>
    
    /**
     * Updates staff information.
     */
    suspend fun updateStaff(
        staffId: String,
        fullName: String? = null,
        phone: String? = null,
        role: String? = null
    ): Result<Unit>

    /**
     * Attendance Methods
     */
    suspend fun getStaffAttendance(staffId: String, month: Int, year: Int): Result<List<Attendance>>
    
    suspend fun getAllStaffAttendance(staffIds: List<String>, month: Int, year: Int): Result<List<Attendance>>
    
    suspend fun submitAttendance(
        staffId: String,
        date: String,
        checkIn: String,
        checkOut: String? = null,
        status: AttendanceStatus = AttendanceStatus.PENDING,
        note: String? = null
    ): Result<Unit>
    
    suspend fun updateAttendanceStatus(attendanceId: String, status: AttendanceStatus): Result<Unit>
    
    suspend fun approveAttendance(attendanceId: String): Result<Unit>
    
    suspend fun updateAttendanceRecord(
        attendanceId: String,
        checkIn: String,
        checkOut: String?,
        status: AttendanceStatus? = null
    ): Result<Unit>

    /**
     * Daily Note Methods
     */

    /**
     * Lấy ghi chú ngày công của một nhân viên trong một tháng.
     */
    suspend fun getDailyNotes(staffId: String, month: Int, year: Int): Result<List<DailyNote>>

    suspend fun getAllDailyNotes(staffIds: List<String>, month: Int, year: Int): Result<List<DailyNote>>

    /**
     * Lưu (thêm mới hoặc cập nhật) ghi chú cho một ngày cụ thể.
     */
    suspend fun upsertDailyNote(staffId: String, noteDate: String, content: String): Result<Unit>

    /**
     * Xóa ghi chú ngày công.
     */
    suspend fun deleteDailyNote(noteId: String): Result<Unit>
}
