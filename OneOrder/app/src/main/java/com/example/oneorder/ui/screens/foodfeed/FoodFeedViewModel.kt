package com.example.oneorder.ui.screens.foodfeed

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.FeedPost
import com.example.oneorder.data.repository.FoodFeedRepository
import com.example.oneorder.data.repository.AuthRepository
import com.example.oneorder.data.repository.FollowingRepository
import com.example.oneorder.utils.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodFeedUiState(
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLastPage: Boolean = false,
    val error: String? = null,
    val showCommentSheet: Boolean = false,
    val commentsForPostId: String? = null,
    val commentInput: String = "",
    val comments: List<com.example.oneorder.data.model.FeedCommentUI> = emptyList(),
    // GPS state để hiển thị trên UI nếu cần
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val locationAvailable: Boolean = false,
    val showOnboarding: Boolean = false,
    /** Index of the post the user was viewing — used to restore VerticalPager position on return */
    val currentPageIndex: Int = 0,
    /** ID of the current post — more stable than index for restoration after feed refresh */
    val currentPostId: String? = null
)

@HiltViewModel
class FoodFeedViewModel @Inject constructor(
    private val repository: FoodFeedRepository,
    private val authRepository: AuthRepository,
    private val followingRepository: FollowingRepository,
    private val locationProvider: LocationProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodFeedUiState())
    val uiState: StateFlow<FoodFeedUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 10

    /**
     * Tracks whether an initial load has already been triggered.
     * Used to prevent fetching with lat=0/lng=0 before the permission dialog
     * has even been presented to the user.
     */
    private var hasInitialLoadBeenTriggered = false

    init {
        // Restore page position from SavedStateHandle (survives process death + navigation)
        savedStateHandle.get<Int>("current_page_index")?.let { savedIndex ->
            _uiState.update { it.copy(currentPageIndex = savedIndex.coerceAtLeast(0)) }
        }
        savedStateHandle.get<String>("current_post_id")?.let { savedId ->
            _uiState.update { it.copy(currentPostId = savedId) }
        }
        checkOnboarding()
        observeFollowingChanges()
    }

    /**
     * Call from Screen composable whenever the visible page changes.
     * Saves position to SavedStateHandle so it persists across navigation and process death.
     */
    fun onPageChanged(pageIndex: Int, posts: List<FeedPost>) {
        val postId = posts.getOrNull(pageIndex)?.id
        savedStateHandle["current_page_index"] = pageIndex
        postId?.let { savedStateHandle["current_post_id"] = it }
        _uiState.update { it.copy(currentPageIndex = pageIndex, currentPostId = postId) }
    }

    /**
     * Returns the page index to restore to when returning to Feed.
     * Prefers matching the saved post ID (more stable), falls back to saved index.
     */
    fun getRestorePageIndex(posts: List<FeedPost>): Int {
        val state = _uiState.value
        // Try to find the post by ID first (stable across feed refreshes)
        state.currentPostId?.let { savedId ->
            val index = posts.indexOfFirst { it.id == savedId }
            if (index >= 0) return index
        }
        // Fall back to saved index
        return state.currentPageIndex.coerceIn(0, (posts.size - 1).coerceAtLeast(0))
    }

    private fun checkOnboarding() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                val exists = repository.checkUserProfileExists(user.id)
                if (!exists) {
                    _uiState.update { it.copy(showOnboarding = true) }
                }
            }
        }
    }

    fun submitOnboardingTags(selectedTags: List<Pair<String, String>>) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null && selectedTags.isNotEmpty()) {
                repository.saveInitialUserProfile(user.id, selectedTags)
                _uiState.update { it.copy(showOnboarding = false) }
                // Xóa cache và trộn lại feed với sở thích mới
                loadRecommendations(forceRefresh = true)
            } else {
                _uiState.update { it.copy(showOnboarding = false) }
            }
        }
    }

    /**
     * Lấy GPS tự động qua [LocationProvider] rồi gọi recommendation API.
     * Nếu permission chưa được cấp, gọi [onPermissionNeeded] để Screen
     * kích hoạt dialog xin quyền từ phía UI (vì permission phải request từ Composable/Activity).
     *
     * QUAN TRỌNG: Khi chưa có quyền, KHÔNG fetch feed với lat=0/lng=0 ngay lập tức.
     * Việc fetch chỉ xảy ra SAU khi user đã trả lời dialog (đồng ý hoặc từ chối),
     * tránh feed load với khoảng cách 0 km trong khi dialog vẫn đang hiển thị.
     *
     * @param onPermissionNeeded callback được gọi nếu chưa có quyền location
     * @param forceRefresh buộc refresh feed (bỏ qua guard early-return)
     */
    fun loadRecommendations(onPermissionNeeded: (() -> Unit)? = null, forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.posts.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Kiểm tra permission
            if (!locationProvider.hasLocationPermission()) {
                // Nếu user chưa từng được hỏi quyền (chưa trigger dialog lần nào),
                // chỉ show dialog và CHƯA fetch feed. Việc fetch sẽ xảy ra khi
                // LaunchedEffect bên Screen nhận thấy trạng thái permission thay đổi.
                if (!hasInitialLoadBeenTriggered) {
                    hasInitialLoadBeenTriggered = true
                    onPermissionNeeded?.invoke()
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                // User đã trả lời dialog rồi (từ chối hoặc đồng ý lần sau).
                // Fetch feed để hiển thị nội dung dù không có GPS.
                onPermissionNeeded?.invoke()
                fetchFeed(lat = 0.0, lng = 0.0, isLoadMore = false)
                return@launch
            }

            // 2. Lấy tọa độ GPS
            val location = locationProvider.getCurrentLocation()
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            _uiState.update {
                it.copy(
                    userLat = lat,
                    userLng = lng,
                    locationAvailable = location != null
                )
            }

            // 3. Fetch recommendations với tọa độ thật (trang đầu tiên)
            currentPage = 0
            _uiState.update { it.copy(isLastPage = false) }
            fetchFeed(lat, lng, isLoadMore = false)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.isLastPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            currentPage++
            fetchFeed(state.userLat, state.userLng, isLoadMore = true)
        }
    }

    private suspend fun fetchFeed(lat: Double, lng: Double, isLoadMore: Boolean) {
        val userId = authRepository.getCurrentUser()?.id
        val currentOffset = currentPage * pageSize

        // #region agent debug log
        Log.d("FoodFeedVM", ">>> [DEBUG] fetchFeed: lat=$lat, lng=$lng, userId=$userId, offset=$currentOffset, isLoadMore=$isLoadMore")
        // #endregion

                repository.getRecommendations(lat, lng, limit = pageSize, offset = currentOffset, userId = userId)
            .onSuccess { newPosts ->
                // #region agent debug log
                Log.d("FoodFeedVM", ">>> [DEBUG] fetchFeed success: postsCount=${newPosts.size}, firstPostDistanceKm=${newPosts.firstOrNull()?.distanceKm}")
                // #endregion
                _uiState.update { state ->
                    val updatedPosts = if (isLoadMore) state.posts + newPosts else newPosts
                    // Restore the saved page position after loading new data.
                    // Use saved post ID to find matching post in potentially refreshed feed.
                    val savedPostId = savedStateHandle.get<String>("current_post_id")
                    val restoredIndex = if (!isLoadMore && savedPostId != null) {
                        updatedPosts.indexOfFirst { it.id == savedPostId }.coerceAtLeast(0)
                    } else {
                        state.currentPageIndex
                    }
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        posts = updatedPosts,
                        isLastPage = newPosts.size < pageSize,
                        currentPageIndex = restoredIndex,
                        currentPostId = savedPostId ?: state.currentPostId
                    )
                }
                // Check isFollowing status for each post's restaurant
                checkFollowingStatus(newPosts)
            }
            .onFailure { error ->
                // #region agent debug log
                Log.e("FoodFeedVM", ">>> [DEBUG] fetchFeed failed: ${error.message}, type=${error.javaClass.simpleName}")
                // #endregion
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = error.message) }
            }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                isLiked = !post.isLiked,
                                likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                            )
                        } else post
                    }
                )
            }
            
            val isLikedNow = _uiState.value.posts.find { it.id == postId }?.isLiked ?: false
            if (user != null) {
                repository.toggleLike(postId, isLikedNow, user.id)
                // Log LIKE interaction
                if (isLikedNow) {
                    repository.logInteraction(postId, user.id, "LIKE", 0.8)
                }
            }
        }
    }

    fun openComments(postId: String) {
        _uiState.update { it.copy(showCommentSheet = true, commentsForPostId = postId) }
        loadComments(postId)
    }

    private fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getComments(postId).onSuccess { loadedComments ->
                _uiState.update { it.copy(comments = loadedComments) }
            }
        }
    }

    fun closeComments() {
        _uiState.update { it.copy(showCommentSheet = false, commentsForPostId = null, comments = emptyList()) }
    }

    fun updateCommentInput(text: String) {
        _uiState.update { it.copy(commentInput = text) }
    }

    fun submitComment() {
        val postId = _uiState.value.commentsForPostId ?: return
        val content = _uiState.value.commentInput
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                repository.addComment(postId, user.id, content)
                loadComments(postId) // Reload
                _uiState.update { it.copy(commentInput = "") }
            }
        }
    }

    fun logInteraction(postId: String, actionType: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            val weight = when (actionType) {
                "VIEW" -> 0.2
                "CLICK" -> 0.5
                "LIKE" -> 0.8
                "ORDER" -> 1.0
                else -> 0.0
            }
            if (weight > 0) {
                repository.logInteraction(postId, user.id, actionType, weight)
            }
        }
    }

    fun followRestaurant(tenantId: String) {
        viewModelScope.launch {
            followingRepository.followRestaurant(tenantId)
            followingRepository.invalidateCache()
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { post ->
                        if (post.restaurantId == tenantId) post.copy(isFollowing = true) else post
                    }
                )
            }
        }
    }

    fun unfollowRestaurant(tenantId: String) {
        viewModelScope.launch {
            followingRepository.unfollowRestaurant(tenantId)
            followingRepository.invalidateCache()
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { post ->
                        if (post.restaurantId == tenantId) post.copy(isFollowing = false) else post
                    }
                )
            }
        }
    }

    /**
     * Observe followed tenant IDs from FollowingRepository's real-time stream.
     * When the user follows/unfollows from another screen (e.g. Profile page),
     * this flow automatically emits the updated set and keeps feed UI in sync.
     */
    private fun observeFollowingChanges() {
        viewModelScope.launch {
            followingRepository.getFollowedTenantIdsFlow().collect { followedIds ->
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.map { post ->
                            if (post.restaurantId != null) {
                                post.copy(isFollowing = followedIds.contains(post.restaurantId))
                            } else post
                        }
                    )
                }
            }
        }
    }

    private fun checkFollowingStatus(posts: List<FeedPost>) {
        viewModelScope.launch {
            posts.forEach { post ->
                post.restaurantId?.let { tenantId ->
                    followingRepository.isFollowing(tenantId)
                        .onSuccess { isFollowing ->
                            _uiState.update { state ->
                                state.copy(
                                    posts = state.posts.map { p ->
                                        if (p.restaurantId == tenantId) p.copy(isFollowing = isFollowing) else p
                                    }
                                )
                            }
                        }
                }
            }
        }
    }
}
