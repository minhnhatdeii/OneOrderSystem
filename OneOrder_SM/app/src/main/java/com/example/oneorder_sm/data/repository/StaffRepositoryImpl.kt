package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.BuildConfig
import com.example.oneorder_sm.domain.model.Attendance
import com.example.oneorder_sm.domain.model.AttendanceStatus
import com.example.oneorder_sm.domain.model.DailyNote
import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.StaffRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@Serializable
private data class StaffListItem(
    val id: String,
    val full_name: String?,
    val phone_number: String?,
    val role: String,
    val email: String?,
    val is_active: Boolean,
    val created_at: String?,
    val created_by_name: String?,
    val avatar_url: String? = null
)

class StaffRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : StaffRepository {

    companion object {
        const val TEMP_PASSWORD = "123456"
    }

    override suspend fun getStaffList(): Result<List<Profile>> {
        return try {
            // Use the get_tenant_staff function which handles tenant filtering
            val staffItems = supabase.postgrest.rpc("get_tenant_staff")
                .decodeList<StaffListItem>()
            
            // Convert to Profile objects
            val profiles = staffItems.map { item ->
                Profile(
                    id = item.id,
                    tenantId = null, // Not returned by function
                    fullName = item.full_name,
                    role = item.role,
                    phoneNumber = item.phone_number,
                    isActive = item.is_active,
                    createdAt = item.created_at,
                    avatarUrl = item.avatar_url
                )
            }
            
            Result.success(profiles)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getCurrentStaffProfile(): Result<Profile> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<Profile>()
                
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Creates a staff account by calling Edge Function.
     * Edge Function will:
     * 1. Create user with Admin API
     * 2. Send invitation email
     * 3. Create profile record
     */
    override suspend fun createStaffAccount(
        email: String,
        fullName: String,
        phone: String?,
        role: String
    ): Result<String> {
        return try {
            // Get current user and tenant info
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<com.example.oneorder_sm.data.model.ProfileWithTenant>()
            
            val tenantId = profile?.tenantId
                ?: return Result.failure(Exception("Manager has no tenant"))
            
            // Call Edge Function to create staff account
            val params = buildJsonObject {
                put("email", email)
                put("fullName", fullName)
                phone?.let { put("phone", it) }
                put("role", role)
                put("tenantId", tenantId)
                put("createdBy", currentUser.id)
            }
            
            // Call Edge Function using Supabase HTTP client
            val response = supabase.httpClient.post(
                "${BuildConfig.SUPABASE_URL}/functions/v1/create-staff-account"
            ) {
                headers {
                    append("Authorization", "Bearer ${supabase.auth.currentAccessTokenOrNull()}")
                    append("Content-Type", "application/json")
                }
                setBody(params.toString())
            }
            
            if (response.status.value in 200..299) {
                Result.success("Tạo tài khoản nhân viên thành công!\n\nEmail: $email\nMật khẩu mặc định: 123456\n\nNhân viên có thể đổi mật khẩu trong Hồ sơ sau khi đăng nhập.")
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Failed to create account: $errorBody"))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deactivateStaff(staffId: String): Result<Unit> {
        return try {
            val params = buildJsonObject {
                put("p_staff_id", staffId)
            }
            
            supabase.postgrest.rpc("deactivate_staff", params)
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun reactivateStaff(staffId: String): Result<Unit> {
        return try {
            val params = buildJsonObject {
                put("p_staff_id", staffId)
            }
            
            supabase.postgrest.rpc("reactivate_staff", params)
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateStaff(
        staffId: String,
        fullName: String?,
        phone: String?,
        role: String?
    ): Result<Unit> {
        return try {
            val updates = buildMap<String, Any> {
                fullName?.let { put("full_name", it) }
                phone?.let { put("phone_number", it) }
                role?.let { put("role", it) }
            }
            
            if (updates.isEmpty()) {
                return Result.success(Unit)
            }
            
            supabase.postgrest.from("profiles")
                .update(updates) {
                    filter { eq("id", staffId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getStaffAttendance(
        staffId: String,
        month: Int,
        year: Int
    ): Result<List<Attendance>> {
        return try {
            val startDate = String.format("%04d-%02d-01", year, month)
            val nextMonth = if (month == 12) 1 else month + 1
            val nextYear = if (month == 12) year + 1 else year
            val endDate = String.format("%04d-%02d-01", nextYear, nextMonth)

            val attendanceItems = supabase.postgrest.from("attendance")
                .select(Columns.ALL) {
                    filter {
                        eq("staff_id", staffId)
                        gte("attendance_date", startDate)
                        lt("attendance_date", endDate)
                    }
                }
                .decodeList<Attendance>()
            
            Result.success(attendanceItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getAllStaffAttendance(
        staffIds: List<String>,
        month: Int,
        year: Int
    ): Result<List<Attendance>> {
        return try {
            if (staffIds.isEmpty()) return Result.success(emptyList())

            val startDate = String.format("%04d-%02d-01", year, month)
            val nextMonth = if (month == 12) 1 else month + 1
            val nextYear = if (month == 12) year + 1 else year
            val endDate = String.format("%04d-%02d-01", nextYear, nextMonth)

            val attendanceItems = supabase.postgrest.from("attendance")
                .select(Columns.ALL) {
                    filter {
                        isIn("staff_id", staffIds)
                        gte("attendance_date", startDate)
                        lt("attendance_date", endDate)
                    }
                }
                .decodeList<Attendance>()
            
            Result.success(attendanceItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun submitAttendance(
        staffId: String,
        date: String,
        checkIn: String,
        checkOut: String?,
        status: AttendanceStatus,
        note: String?
    ): Result<Unit> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<com.example.oneorder_sm.data.model.ProfileWithTenant>()
            
            val tenantId = profile?.tenantId
                ?: return Result.failure(Exception("Has no tenant"))

            val data = Attendance(
                staffId = staffId,
                tenantId = tenantId,
                date = date,
                checkIn = checkIn,
                checkOut = checkOut,
                status = status,
                note = note
            )

            supabase.postgrest.from("attendance").insert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun approveAttendance(attendanceId: String): Result<Unit> {
        return try {
            supabase.postgrest.from("attendance").update(mapOf("status" to "approved")) {
                filter { eq("id", attendanceId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateAttendanceStatus(attendanceId: String, status: AttendanceStatus): Result<Unit> {
        return try {
            supabase.postgrest.from("attendance").update(mapOf("status" to status.name.lowercase())) {
                filter { eq("id", attendanceId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateAttendanceRecord(
        attendanceId: String,
        checkIn: String,
        checkOut: String?,
        status: AttendanceStatus?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "check_in" to checkIn,
                "updated_at" to "now()"
            )
            checkOut?.let { updates["check_out"] = it }
            status?.let { updates["status"] = it.name.lowercase() }

            supabase.postgrest.from("attendance").update(updates) {
                filter { eq("id", attendanceId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // -------------------------
    // Daily Note Methods
    // -------------------------

    override suspend fun getDailyNotes(staffId: String, month: Int, year: Int): Result<List<DailyNote>> {
        return try {
            val startDate = String.format("%04d-%02d-01", year, month)
            val nextMonth = if (month == 12) 1 else month + 1
            val nextYear = if (month == 12) year + 1 else year
            val endDate = String.format("%04d-%02d-01", nextYear, nextMonth)

            val notes = supabase.postgrest.from("attendance_daily_notes")
                .select(Columns.ALL) {
                    filter {
                        eq("staff_id", staffId)
                        gte("note_date", startDate)
                        lt("note_date", endDate)
                    }
                }
                .decodeList<DailyNote>()

            Result.success(notes)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getAllDailyNotes(staffIds: List<String>, month: Int, year: Int): Result<List<DailyNote>> {
        return try {
            if (staffIds.isEmpty()) return Result.success(emptyList())

            val startDate = String.format("%04d-%02d-01", year, month)
            val nextMonth = if (month == 12) 1 else month + 1
            val nextYear = if (month == 12) year + 1 else year
            val endDate = String.format("%04d-%02d-01", nextYear, nextMonth)

            val notes = supabase.postgrest.from("attendance_daily_notes")
                .select(Columns.ALL) {
                    filter {
                        isIn("staff_id", staffIds)
                        gte("note_date", startDate)
                        lt("note_date", endDate)
                    }
                }
                .decodeList<DailyNote>()

            Result.success(notes)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun upsertDailyNote(staffId: String, noteDate: String, content: String): Result<Unit> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))

            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<com.example.oneorder_sm.data.model.ProfileWithTenant>()

            val tenantId = profile?.tenantId
                ?: return Result.failure(Exception("Has no tenant"))

            // Kiểm tra xem note đã tồn tại chưa
            val existing = supabase.postgrest.from("attendance_daily_notes")
                .select {
                    filter {
                        eq("staff_id", staffId)
                        eq("note_date", noteDate)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<DailyNote>()

            if (existing != null && existing.id != null) {
                // Update note hiện có
                supabase.postgrest.from("attendance_daily_notes").update(
                    mapOf("content" to content, "updated_at" to "now()")
                ) {
                    filter { eq("id", existing.id) }
                }
            } else {
                // Insert note mới
                val note = DailyNote(
                    staffId = staffId,
                    tenantId = tenantId,
                    noteDate = noteDate,
                    content = content,
                    createdBy = currentUser.id
                )
                supabase.postgrest.from("attendance_daily_notes").insert(note)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteDailyNote(noteId: String): Result<Unit> {
        return try {
            supabase.postgrest.from("attendance_daily_notes").delete {
                filter { eq("id", noteId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
