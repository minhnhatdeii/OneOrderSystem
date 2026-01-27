package com.example.oneorder_sm.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            profileRepository.getCurrentProfile()
                .onSuccess { (profile, email) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            email = email,
                            editFullName = profile.fullName ?: "",
                            editPhone = profile.phoneNumber ?: ""
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load profile"
                        )
                    }
                }
        }
    }
    
    fun startEditing() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editFullName = profile.fullName ?: "",
                editPhone = profile.phoneNumber ?: ""
            )
        }
    }
    
    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }
    
    fun updateFullName(value: String) {
        _uiState.update { it.copy(editFullName = value) }
    }
    
    fun updatePhone(value: String) {
        _uiState.update { it.copy(editPhone = value) }
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            profileRepository.updateProfile(
                fullName = _uiState.value.editFullName,
                phoneNumber = _uiState.value.editPhone.takeIf { it.isNotBlank() }
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = false,
                            successMessage = "Profile updated successfully"
                        )
                    }
                    loadProfile() // Reload to get updated data
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to update profile"
                        )
                    }
                }
        }
    }
    
    fun showPasswordDialog() {
        _uiState.update {
            it.copy(
                showPasswordDialog = true,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                passwordError = null
            )
        }
    }
    
    fun hidePasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false) }
    }
    
    fun updateCurrentPassword(value: String) {
        _uiState.update { it.copy(currentPassword = value) }
    }
    
    fun updateNewPassword(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }
    
    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }
    
    fun changePassword() {
        val state = _uiState.value
        
        // Validation
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(passwordError = "Please enter current password") }
            return
        }
        
        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(passwordError = "New password must be at least 6 characters") }
            return
        }
        
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(passwordError = "Passwords do not match") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, passwordError = null) }
            
            profileRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPasswordDialog = false,
                            successMessage = "Password changed successfully"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordError = error.message ?: "Failed to change password"
                        )
                    }
                }
        }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
