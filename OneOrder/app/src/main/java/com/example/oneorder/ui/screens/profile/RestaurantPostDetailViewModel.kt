package com.example.oneorder.ui.screens.profile

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.FeedCommentUI
import com.example.oneorder.data.model.FeedPost
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.data.repository.RestaurantProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class SimpleMenuItemDto(
    val id: Long,
    val name: String,
    val price: Double
)

data class RestaurantPostDetailUiState(
    val isLoading: Boolean = true,
    val posts: List<FeedPost> = emptyList(),
    val initialPageIndex: Int = 0,
    val showCommentSheet: Boolean = false,
    val commentsForPostId: String? = null,
    val comments: List<FeedCommentUI> = emptyList(),
    val commentInput: String = "",
    val error: String? = null
)

@HiltViewModel
class RestaurantPostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RestaurantProfileRepository,
    private val foodFeedRepository: com.example.oneorder.data.repository.FoodFeedRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantPostDetailUiState())
    val uiState: StateFlow<RestaurantPostDetailUiState> = _uiState.asStateFlow()

    fun loadPosts(tenantId: String, initialPostId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val profileResult = repository.getRestaurantProfile(tenantId)
                val postsResult = repository.getRestaurantPostsFullList(tenantId)
                
                if (profileResult.isSuccess && postsResult.isSuccess) {
                    val profile = profileResult.getOrThrow()
                    val postsListDto = postsResult.getOrThrow()
                    
                    // Fetch all menu items for the restaurant to map names and prices
                    val itemIds = postsListDto.mapNotNull { it.menuItemId }.distinct()
                    val menuItemsMap = mutableMapOf<Long, SimpleMenuItemDto>()
                    if (itemIds.isNotEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                val menuItems = supabase.postgrest.from("menu_items")
                                    .select { filter { isIn("id", itemIds) } }
                                    .decodeList<SimpleMenuItemDto>()
                                
                                menuItems.forEach { item ->
                                    menuItemsMap[item.id] = item
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("PostDetailVM", "Error fetching menu items for tenant $tenantId", e)
                        }
                    }
                    
                    val feedPosts = postsListDto.map { postDto ->
                        val menuItem = postDto.menuItemId?.let { menuItemsMap[it] }
                        
                        FeedPost(
                            id = postDto.id,
                            restaurantId = profile.tenantId,
                            restaurantName = profile.restaurantName,
                            restaurantAddress = profile.address,
                            restaurantLat = 0.0, // GPS distance is omitted for performance in profile
                            restaurantLng = 0.0,
                            distanceKm = 0.0,
                            restaurantAvatar = profile.avatarUrl,
                            menuItemName = menuItem?.name ?: "Món ăn",
                            images = postDto.images ?: emptyList(),
                            caption = postDto.caption ?: "",
                            price = menuItem?.price ?: 0.0,
                            likeCount = postDto.likeCount,
                            commentCount = 0, // Mock
                            shareCount = 0, // Mock
                            isLiked = false // Mock or fetch actual like status if needed
                        )
                    }
                    
                    // Find the index of the clicked post
                    var initialIndex = feedPosts.indexOfFirst { it.id == initialPostId }
                    if (initialIndex == -1) initialIndex = 0
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            posts = feedPosts,
                            initialPageIndex = initialIndex
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Không thể tải bài đăng") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentPosts = _uiState.value.posts.toMutableList()
        val index = currentPosts.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = currentPosts[index]
            val isLikedNow = !post.isLiked
            val newLikeCount = if (isLikedNow) post.likeCount + 1 else post.likeCount - 1
            
            currentPosts[index] = post.copy(
                isLiked = isLikedNow,
                likeCount = newLikeCount
            )
            _uiState.update { it.copy(posts = currentPosts) }
            
            viewModelScope.launch {
                try {
                    val user = supabase.auth.currentUserOrNull()
                    if (user != null) {
                        foodFeedRepository.toggleLike(postId, isLikedNow, user.id)
                    }
                } catch (e: Exception) {
                    Log.w("PostDetailVM", "Failed to toggle like", e)
                }
            }
        }
    }

    fun openComments(postId: String) {
        _uiState.update { it.copy(showCommentSheet = true, commentsForPostId = postId) }
        viewModelScope.launch {
            val result = foodFeedRepository.getComments(postId)
            if (result.isSuccess) {
                _uiState.update { it.copy(comments = result.getOrDefault(emptyList())) }
            }
        }
    }

    fun closeComments() {
        _uiState.update { it.copy(showCommentSheet = false, commentsForPostId = null, comments = emptyList()) }
    }

    fun updateCommentInput(input: String) {
        _uiState.update { it.copy(commentInput = input) }
    }

    fun submitComment() {
        val postId = _uiState.value.commentsForPostId ?: return
        val content = _uiState.value.commentInput
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    val result = foodFeedRepository.addComment(postId, user.id, content)
                    if (result.isSuccess) {
                        _uiState.update { it.copy(commentInput = "") }
                        // Refresh comments
                        val freshComments = foodFeedRepository.getComments(postId)
                        if (freshComments.isSuccess) {
                             _uiState.update { it.copy(comments = freshComments.getOrDefault(emptyList())) }
                        }
                        
                        // Update comment count in post
                        val currentPosts = _uiState.value.posts.toMutableList()
                        val index = currentPosts.indexOfFirst { it.id == postId }
                        if (index != -1) {
                            currentPosts[index] = currentPosts[index].copy(commentCount = currentPosts[index].commentCount + 1)
                            _uiState.update { it.copy(posts = currentPosts) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PostDetailVM", "Error submitting comment", e)
            }
        }
    }
}
