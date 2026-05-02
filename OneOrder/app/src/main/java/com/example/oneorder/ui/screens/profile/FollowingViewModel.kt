package com.example.oneorder.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.FollowedRestaurant
import com.example.oneorder.data.repository.FollowingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowingUiState(
    val followedRestaurants: List<FollowedRestaurant> = emptyList(),
    val filteredRestaurants: List<FollowedRestaurant> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val followingRepository: FollowingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()

    init {
        loadFollowedRestaurants()
    }

    fun loadFollowedRestaurants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            followingRepository.getFollowedRestaurants()
                .onSuccess { list ->
                    _uiState.update { state ->
                        state.copy(
                            followedRestaurants = list,
                            filteredRestaurants = filterList(list, state.searchQuery),
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    val errorMsg = when {
                        e.message?.contains("followed_restaurants") == true ->
                            "Tính năng theo dõi chưa được kích hoạt"
                        e.message?.contains("Could not find") == true ->
                            "Tính năng theo dõi chưa được kích hoạt"
                        else -> e.message ?: "Không thể tải danh sách theo dõi"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredRestaurants = filterList(state.followedRestaurants, query)
            )
        }
    }

    fun unfollowRestaurant(tenantId: String) {
        viewModelScope.launch {
            followingRepository.unfollowRestaurant(tenantId)
                .onSuccess {
                    _uiState.update { state ->
                        val updated = state.followedRestaurants.filter { it.tenantId != tenantId }
                        state.copy(
                            followedRestaurants = updated,
                            filteredRestaurants = filterList(updated, state.searchQuery)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Không thể hủy theo dõi")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterList(list: List<FollowedRestaurant>, query: String): List<FollowedRestaurant> {
        if (query.isBlank()) return list
        return list.filter {
            it.restaurantName.contains(query, ignoreCase = true) ||
                    it.address?.contains(query, ignoreCase = true) == true
        }
    }
}
