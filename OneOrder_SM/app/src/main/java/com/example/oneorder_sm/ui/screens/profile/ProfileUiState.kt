package com.example.oneorder_sm.ui.screens.profile

import com.example.oneorder_sm.domain.model.Profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val email: String? = null,  // User email from auth
    val error: String? = null,
    
    // Edit mode
    val isEditing: Boolean = false,
    val editFullName: String = "",
    val editPhone: String = "",
    
    // Change password dialog
    val showPasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    
    // Messages
    val successMessage: String? = null
)
