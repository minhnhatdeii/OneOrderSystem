package com.example.oneorder_sm.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantSettingsState(
    val isLoading: Boolean = true,
    val restaurantName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val staffCount: Int = 0,
    val tableCount: Int = 0,
    val menuItemCount: Int = 0,
    val message: String? = null
)

@HiltViewModel
class RestaurantSettingsViewModel @Inject constructor(
    private val tenantRepository: TenantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantSettingsState())
    val uiState: StateFlow<RestaurantSettingsState> = _uiState.asStateFlow()

    init {
        loadRestaurantInfo()
        loadStatistics()
    }

    private fun loadRestaurantInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = tenantRepository.getCurrentTenant()
            if (result.isSuccess) {
                val tenant = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        restaurantName = tenant?.restaurantName ?: "",
                        address = tenant?.address ?: "",
                        phone = tenant?.phoneNumber ?: "",
                        email = tenant?.email ?: ""
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Không thể tải thông tin nhà hàng"
                    )
                }
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val stats = tenantRepository.getTenantStatistics()
            if (stats.isSuccess) {
                val data = stats.getOrNull()
                _uiState.update {
                    it.copy(
                        staffCount = data?.get("staff_count") as? Int ?: 0,
                        tableCount = data?.get("table_count") as? Int ?: 0,
                        menuItemCount = data?.get("menu_item_count") as? Int ?: 0
                    )
                }
            }
        }
    }

    fun updateRestaurantInfo(
        name: String,
        address: String,
        phone: String,
        email: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = tenantRepository.updateTenant(
                name = name,
                address = address.takeIf { it.isNotBlank() },
                phone = phone.takeIf { it.isNotBlank() },
                email = email.takeIf { it.isNotBlank() }
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        restaurantName = name,
                        address = address,
                        phone = phone,
                        email = email,
                        message = "Đã cập nhật thành công"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = result.exceptionOrNull()?.message ?: "Không thể cập nhật"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
