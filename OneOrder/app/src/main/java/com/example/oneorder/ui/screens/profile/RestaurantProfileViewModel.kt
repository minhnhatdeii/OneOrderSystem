package com.example.oneorder.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.RestaurantProfilePost
import com.example.oneorder.data.model.RestaurantStats
import com.example.oneorder.data.repository.FollowingRepository
import com.example.oneorder.data.repository.RestaurantProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantProfileUiState(
    val isLoading: Boolean = true,
    val restaurantId: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val address: String = "",
    val phone: String = "",
    val description: String = "",
    val descriptionAlignment: String = "LEFT",
    val bio: String = "",
    val stats: RestaurantStats = RestaurantStats(0, 0, 0),
    val isFollowing: Boolean = false,
    val isTogglingFollow: Boolean = false,
    val postsGrid: List<RestaurantProfilePost> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class RestaurantProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RestaurantProfileRepository,
    private val followingRepository: FollowingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantProfileUiState())
    val uiState: StateFlow<RestaurantProfileUiState> = _uiState.asStateFlow()

    init {
        // Observe the shared set of followed tenant IDs from the singleton repository.
        // This updates isFollowing reactively whenever follow/unfollow happens anywhere
        // in the app (e.g. from Feed) — no need to reload the whole profile.
        viewModelScope.launch {
            followingRepository.getFollowedTenantIdsFlow().collect { followedIds ->
                val currentId = _uiState.value.restaurantId
                if (currentId.isNotEmpty()) {
                    val nowFollowing = followedIds.contains(currentId)
                    // Only update if state actually changed to avoid unnecessary recompositions
                    if (_uiState.value.isFollowing != nowFollowing) {
                        _uiState.update { it.copy(isFollowing = nowFollowing) }
                    }
                }
            }
        }
    }

    fun loadProfile(restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, restaurantId = restaurantId, error = null) }

            // Load profile data and posts concurrently
            val profileResult = repository.getRestaurantProfile(restaurantId)
            val postsResult = repository.getRestaurantPosts(restaurantId)

            // Get current follow status from the in-memory set (fast, no network call needed
            // if refreshFollowData already ran at startup; fallback to RPC if set is empty)
            val followedIds = followingRepository.getFollowedTenantIdsFlow().value
            val isFollowing = if (followedIds.isNotEmpty()) {
                followedIds.contains(restaurantId)
            } else {
                followingRepository.isFollowing(restaurantId).getOrNull() ?: false
            }

            profileResult.fold(
                onSuccess = { profile ->
                    val posts = postsResult.getOrNull() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = profile.restaurantName.ifEmpty { "Nhà hàng" },
                            avatarUrl = profile.avatarUrl,
                            coverUrl = profile.coverUrl,
                            address = profile.address.ifEmpty { "Chưa cập nhật" },
                            phone = profile.phone.ifEmpty { "Chưa cập nhật" },
                            description = profile.description.ifEmpty { "Chưa cập nhật" },
                            bio = profile.description,
                            stats = profile.stats,
                            isFollowing = isFollowing,
                            postsGrid = posts
                        )
                    }
                },
                onFailure = { _ ->
                    loadMockData(restaurantId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Không thể tải thông tin nhà hàng. Đang hiển thị dữ liệu mẫu."
                        )
                    }
                }
            )
        }
    }

    private fun loadMockData(restaurantId: String) {
        val idHash = restaurantId.hashCode()
        val followers = 1500 + (idHash % 10000 % 8000)
        val likes = followers * 3 + (idHash % 1000)

        val mockImagesUrls = listOf(
            "https://images.unsplash.com/photo-1555126634-323283e090fa?w=800&q=80",
            "https://images.unsplash.com/photo-1547592180-85f173990554?w=800&q=80",
            "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=800&q=80",
            "https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=800&q=80",
            "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=800&q=80",
            "https://images.unsplash.com/photo-1611143669185-af224c5e3252?w=800&q=80",
            "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800&q=80",
            "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=800&q=80",
            "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=800&q=80"
        )

        val mockImages = mockImagesUrls.mapIndexed { index, url ->
            RestaurantProfilePost(
                id = "post_$index",
                imageUrl = url,
                likeCount = 10 + (index * 15 + idHash % 50)
            )
        }

        _uiState.update {
            it.copy(
                name = "Nhà hàng",
                address = "Chưa cập nhật",
                phone = "0909999888",
                description = "Thông tin nhà hàng đang được cập nhật.",
                bio = "Đang cập nhật...",
                stats = RestaurantStats(followers, likes, mockImages.size),
                postsGrid = mockImages.shuffled().take(5 + (idHash % 5))
            )
        }
    }

    fun toggleFollow() {
        val currentState = _uiState.value
        if (currentState.isTogglingFollow) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingFollow = true) }

            val wasFollowing = currentState.isFollowing
            // Optimistic UI update
            _uiState.update {
                it.copy(
                    isFollowing = !wasFollowing,
                    stats = it.stats.copy(
                        followers = if (wasFollowing) (it.stats.followers - 1).coerceAtLeast(0) else it.stats.followers + 1
                    )
                )
            }

            val result = if (wasFollowing) {
                followingRepository.unfollowRestaurant(currentState.restaurantId)
            } else {
                followingRepository.followRestaurant(currentState.restaurantId)
            }

            result.onSuccess {
                // invalidateCache triggers refreshFollowData() which updates
                // the shared followedTenantIdsFlow → this VM's collector above
                // will then sync isFollowing automatically
                followingRepository.invalidateCache()
            }

            result.onFailure {
                // Revert optimistic update on failure
                _uiState.update {
                    it.copy(
                        isFollowing = wasFollowing,
                        stats = it.stats.copy(
                            followers = if (wasFollowing) it.stats.followers + 1 else (it.stats.followers - 1).coerceAtLeast(0)
                        )
                    )
                }
            }

            _uiState.update { it.copy(isTogglingFollow = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

