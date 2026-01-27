package com.example.oneorder_sm.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.domain.repository.AuthRepository
import com.example.oneorder_sm.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val selectedScreen: String = "dashboard", // Default to dashboard, will be updated based on role
    val restaurantName: String = "",
    val userRole: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tenantRepository: TenantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val profile = authRepository.getCurrentProfile()
                val tenant = tenantRepository.getCurrentTenant().getOrNull()
                
                // Set default screen based on role
                val defaultScreen = if (profile?.role == "manager") "dashboard" else "orders"
                
                _uiState.update {
                    it.copy(
                        selectedScreen = defaultScreen,
                        restaurantName = tenant?.restaurantName ?: "Nhà hàng",
                        userRole = profile?.role ?: "staff"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        selectedScreen = "orders",
                        restaurantName = "Nhà hàng",
                        userRole = "staff"
                    )
                }
            }
        }
    }

    fun selectScreen(screenId: String) {
        _uiState.update { it.copy(selectedScreen = screenId) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
