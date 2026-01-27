package com.example.oneorder_sm.ui.screens.staff

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val staff: List<Profile> = emptyList(),
    val error: String? = null,
    val tempPassword: String? = null, // Password to show after creation
    val createdEmail: String? = null // Email of created staff
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
                Log.e(TAG, "Error message: ${error?.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = staffResult.exceptionOrNull()?.message ?: "Failed to load data"
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
}

