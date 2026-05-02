package com.example.oneorder_sm.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AttendanceStatus {
    @SerialName("pending")
    PENDING,
    @SerialName("approved")
    APPROVED,
    @SerialName("rejected")
    REJECTED,
    @SerialName("day_off")
    DAY_OFF
}

/** Loại bản ghi: ca làm việc thông thường hay xin nghỉ */
enum class DayType {
    WORK,
    DAY_OFF
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Attendance(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("staff_id")
    val staffId: String,
    @SerialName("tenant_id")
    val tenantId: String,
    @SerialName("attendance_date")
    val date: String, // Format: YYYY-MM-DD
    @SerialName("check_in")
    val checkIn: String, // ISO 8601 string
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("check_out")
    val checkOut: String? = null, // ISO 8601 string
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val status: AttendanceStatus = AttendanceStatus.PENDING,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("total_hours")
    val totalHours: Double = 0.0,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val note: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null
)
