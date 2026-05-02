package com.example.oneorder_sm.ui.screens.staff

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.domain.model.Attendance
import com.example.oneorder_sm.domain.model.DailyNote
import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffListState(
    val isLoading: Boolean = false,
    val isManager: Boolean = true,
    val staff: List<Profile> = emptyList(),
    val error: String? = null,
    val tempPassword: String? = null, // Password to show after creation
    val createdEmail: String? = null, // Email of created staff
    val timekeepingSuccessMessage: String? = null, // Thông báo khi chấm công xong
    val staffAttendance: Map<String, List<Attendance>> = emptyMap(), // Lưu chấm công
    val staffDailyNotes: Map<String, List<DailyNote>> = emptyMap(), // Lưu ghi chú ngày công
    val noteSuccessMessage: String? = null // Thông báo khi lưu ghi chú xong
)

@HiltViewModel
class StaffManagementViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {

    companion object {
        private const val TAG = "StaffManagementVM"
    }

    private val _uiState = MutableStateFlow(StaffListState())
    val uiState: StateFlow<StaffListState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        loadData()
    }

    fun loadData() {
        Log.d(TAG, "loadData() called")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val staffResult = staffRepository.getStaffList()
            
            if (staffResult.isFailure) {
                val error = staffResult.exceptionOrNull()
                Log.e(TAG, "loadData() failed", error)
                
                // Fallback for staff role: only load their own profile
                if (error?.message?.contains("Only managers can view staff list") == true) {
                    val profileResult = staffRepository.getCurrentStaffProfile()
                    if (profileResult.isSuccess) {
                        val profile = profileResult.getOrNull()
                        if (profile != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isManager = false,
                                    error = null,
                                    staff = listOf(profile)
                                )
                            }
                            return@launch
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error?.message ?: "Failed to load data"
                    )
                }
                return@launch
            }
            
            val staffList = staffResult.getOrDefault(emptyList())
            Log.d(TAG, "loadData() success - loaded ${staffList.size} staff members")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    staff = staffList
                )
            }
            // Fetch initial data for global calendar
            val now = java.time.LocalDate.now()
            fetchAllDataForMonth(now.monthValue, now.year, staffList)
        }
    }

    fun fetchAllDataForMonth(month: Int, year: Int, staffList: List<Profile> = _uiState.value.staff) {
        staffList.forEach { profile ->
            fetchAttendance(profile.id, month, year)
            fetchDailyNotes(profile.id, month, year)
        }
    }

    fun createStaff(email: String, fullName: String, phone: String?, role: String) {
        Log.d(TAG, "createStaff() called - email=$email, fullName=$fullName, role=$role")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, tempPassword = null) }
            
            val result = staffRepository.createStaffAccount(
                email = email,
                fullName = fullName,
                phone = phone,
                role = role
            )
            
            if (result.isSuccess) {
                val password = result.getOrNull()!!
                Log.d(TAG, "createStaff() SUCCESS - account created for $email")
                _uiState.update {
                    it.copy(
                        tempPassword = password,
                        createdEmail = email
                    )
                }
                // Reload staff list
                loadData()
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "createStaff() FAILED", error)
                Log.e(TAG, "Error details: ${error?.message}")
                Log.e(TAG, "Error class: ${error?.javaClass?.simpleName}")
                error?.printStackTrace()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Lỗi tạo tài khoản: ${error?.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun clearTempPassword() {
        Log.d(TAG, "clearTempPassword() called")
        _uiState.update { it.copy(tempPassword = null, createdEmail = null) }
    }

    fun deactivateStaff(staffId: String) {
        Log.d(TAG, "deactivateStaff() called - staffId=$staffId")
        viewModelScope.launch {
            val result = staffRepository.deactivateStaff(staffId)
            
            if (result.isSuccess) {
                Log.d(TAG, "deactivateStaff() SUCCESS")
                // Update local state
                _uiState.update { state ->
                    state.copy(
                        staff = state.staff.map { profile ->
                            if (profile.id == staffId) profile.copy(isActive = false)
                            else profile
                        }
                    )
                }
            } else {
                Log.e(TAG, "deactivateStaff() FAILED", result.exceptionOrNull())
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Không thể vô hiệu hóa")
                }
            }
        }
    }

    fun reactivateStaff(staffId: String) {
        Log.d(TAG, "reactivateStaff() called - staffId=$staffId")
        viewModelScope.launch {
            val result = staffRepository.reactivateStaff(staffId)
            
            if (result.isSuccess) {
                Log.d(TAG, "reactivateStaff() SUCCESS")
                // Update local state
                _uiState.update { state ->
                    state.copy(
                        staff = state.staff.map { profile ->
                            if (profile.id == staffId) profile.copy(isActive = true)
                            else profile
                        }
                    )
                }
            } else {
                Log.e(TAG, "reactivateStaff() FAILED", result.exceptionOrNull())
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Không thể kích hoạt lại")
                }
            }
        }
    }
    fun submitTimekeeping(staffId: String, staffName: String, date: String, note: String) {
        Log.d(TAG, "submitTimekeeping() called: $staffName ($staffId) on $date with note: $note")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Build full ISO 8601 datetime: "2024-04-11" -> "2024-04-11T00:00:00"
            val checkInTimestamp = "${date}T00:00:00"
            val result = staffRepository.submitAttendance(
                staffId = staffId,
                date = date,
                checkIn = checkInTimestamp,
                checkOut = null,
                status = com.example.oneorder_sm.domain.model.AttendanceStatus.PENDING,
                note = note
            )
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        timekeepingSuccessMessage = "Đã gửi yêu cầu chấm công cho $staffName thành công!\nNgày: $date\nGhi chú: $note"
                    ) 
                }
                val now = java.time.LocalDate.now()
                fetchAttendance(staffId, now.monthValue, now.year)
            } else {
                _uiState.update { 
                    it.copy(isLoading = false, error = "Lỗi: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    /** Nhân viên xin nghỉ ngày [date] với lý do [note] */
    fun submitDayOffRequest(staffId: String, staffName: String, date: String, note: String) {
        Log.d(TAG, "submitDayOffRequest() called: $staffName ($staffId) on $date, reason: $note")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Dùng check_in = date + "T00:00:00" để tránh lỗi timestamp
            val checkInTimestamp = "${date}T00:00:00"
            val result = staffRepository.submitAttendance(
                staffId = staffId,
                date = date,
                checkIn = checkInTimestamp,
                checkOut = null,
                status = com.example.oneorder_sm.domain.model.AttendanceStatus.DAY_OFF,
                note = note
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        timekeepingSuccessMessage = "Đã gửi đơn xin nghỉ cho $staffName!\nNgày: $date\nLý do: $note"
                    )
                }
                val now = java.time.LocalDate.now()
                fetchAttendance(staffId, now.monthValue, now.year)
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Lỗi gửi đơn xin nghỉ: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun fetchAttendance(staffId: String, month: Int, year: Int) {
        viewModelScope.launch {
            staffRepository.getStaffAttendance(staffId, month, year)
                .onSuccess { list ->
                    _uiState.update { state ->
                        val updatedMap = state.staffAttendance.toMutableMap()
                        updatedMap[staffId] = list
                        state.copy(staffAttendance = updatedMap)
                    }
                }
        }
    }

    fun approveAttendance(staffId: String, attendanceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            staffRepository.approveAttendance(attendanceId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    // Refresh
                    val now = java.time.LocalDate.now()
                    fetchAttendance(staffId, now.monthValue, now.year)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun rejectAttendance(staffId: String, attendanceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            staffRepository.updateAttendanceStatus(attendanceId, com.example.oneorder_sm.domain.model.AttendanceStatus.REJECTED)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    // Refresh
                    val now = java.time.LocalDate.now()
                    fetchAttendance(staffId, now.monthValue, now.year)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun clearTimekeepingMessage() {
        _uiState.update { it.copy(timekeepingSuccessMessage = null) }
    }

    // -------------------------
    // Daily Note Functions
    // -------------------------

    fun fetchDailyNotes(staffId: String, month: Int, year: Int) {
        viewModelScope.launch {
            staffRepository.getDailyNotes(staffId, month, year)
                .onSuccess { list ->
                    _uiState.update { state ->
                        val updatedMap = state.staffDailyNotes.toMutableMap()
                        updatedMap[staffId] = list
                        state.copy(staffDailyNotes = updatedMap)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "fetchDailyNotes() FAILED", error)
                }
        }
    }

    fun saveNote(staffId: String, staffName: String, noteDate: String, content: String) {
        Log.d(TAG, "saveNote() called: staffName=$staffName on $noteDate")
        viewModelScope.launch {
            val result = staffRepository.upsertDailyNote(
                staffId = staffId,
                noteDate = noteDate,
                content = content
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(noteSuccessMessage = "Đã lưu ghi chú ngày $noteDate cho $staffName")
                }
                // Refresh notes for that month
                val parts = noteDate.split("-")
                if (parts.size == 3) {
                    fetchDailyNotes(staffId, parts[1].toInt(), parts[0].toInt())
                }
            } else {
                _uiState.update {
                    it.copy(error = "Lỗi lưu ghi chú: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun deleteNote(staffId: String, noteId: String, noteDate: String) {
        Log.d(TAG, "deleteNote() called: noteId=$noteId")
        viewModelScope.launch {
            val result = staffRepository.deleteDailyNote(noteId)
            if (result.isSuccess) {
                val parts = noteDate.split("-")
                if (parts.size == 3) {
                    fetchDailyNotes(staffId, parts[1].toInt(), parts[0].toInt())
                }
            } else {
                _uiState.update {
                    it.copy(error = "Lỗi xóa ghi chú: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun clearNoteSuccessMessage() {
        _uiState.update { it.copy(noteSuccessMessage = null) }
    }
}

