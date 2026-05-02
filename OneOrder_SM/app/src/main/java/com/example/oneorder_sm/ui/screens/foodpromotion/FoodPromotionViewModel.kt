package com.example.oneorder_sm.ui.screens.foodpromotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.domain.model.Tenant
import com.example.oneorder_sm.domain.repository.StaffRepository
import com.example.oneorder_sm.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.oneorder_sm.data.repository.FoodPostRepository
import com.example.oneorder_sm.data.repository.FeedCommentUI

data class FoodPost(
    val id: String,
    val menuItemName: String,
    val images: List<PostImage> = emptyList(), // Danh sách ảnh thay vì chỉ 1 ảnh
    val caption: String,
    val likeCount: Int,
    val commentCount: Int,
    val isActive: Boolean,
    val postedAt: String,
    val price: Double,
    val categoryName: String,
    val isLiked: Boolean = false
)

@kotlinx.serialization.Serializable
data class PostImage(
    val url: String,
    val layout: String = "PORTRAIT" // PORTRAIT, SQUARE, LANDSCAPE, TALL
)

data class FoodPromotionUiState(
    val posts: List<FoodPost> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,

    // Feed view state
    val feedStartIndex: Int = 0,
    val showCommentSheet: Boolean = false,
    val commentsForPostId: String? = null,
    val comments: List<FeedCommentUI> = emptyList(),
    val commentInput: String = "",

    // Real restaurant profile data
    val tenant: Tenant? = null,
    val restaurantName: String = "",
    val restaurantId: String = "",       // derived handle from name
    val bio: String = "",
    val description: String = "",
    val descriptionAlignment: String = "LEFT", // LEFT, CENTER, RIGHT
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val followers: Int = 0,

    // Profile Edit
    val showEditProfileDialog: Boolean = false,
    val editProfileError: String? = null,
    val isSavingProfile: Boolean = false,

    // Add Staff
    val showAddStaffDialog: Boolean = false,
    val tempPassword: String? = null,
    val createdEmail: String? = null,
    val addStaffError: String? = null
)



@HiltViewModel
class FoodPromotionViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val tenantRepository: TenantRepository,
    private val foodPostRepository: FoodPostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodPromotionUiState())
    val uiState: StateFlow<FoodPromotionUiState> = _uiState.asStateFlow()

    init {
        loadRestaurantProfile()
    }

    // -------------------------
    // Load real restaurant data
    // -------------------------

    fun loadRestaurantProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = tenantRepository.getCurrentTenant()
            result.onSuccess { tenant ->
                if (tenant != null) {
                    val rawDesc = tenant.description ?: ""
                    var align = "LEFT"
                    var cleanDesc = rawDesc
                    if (rawDesc.startsWith("[CENTER]")) {
                        align = "CENTER"
                        cleanDesc = rawDesc.removePrefix("[CENTER]")
                    } else if (rawDesc.startsWith("[RIGHT]")) {
                        align = "RIGHT"
                        cleanDesc = rawDesc.removePrefix("[RIGHT]")
                    } else if (rawDesc.startsWith("[LEFT]")) {
                        align = "LEFT"
                        cleanDesc = rawDesc.removePrefix("[LEFT]")
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tenant = tenant,
                            restaurantName = tenant.restaurantName,
                            restaurantId = tenant.restaurantName.toHandle(),
                            address = tenant.address ?: "",
                            phone = tenant.phoneNumber ?: "",
                            email = tenant.email ?: "",
                            bio = buildBioFromTenant(tenant),
                            description = cleanDesc,
                            descriptionAlignment = align,
                            avatarUrl = tenant.logoUrl,
                            coverUrl = tenant.coverUrl
                        )
                    }
                    loadPosts(tenant.id) // Fetch posts for tenant
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    private fun buildBioFromTenant(tenant: Tenant): String {
        return when {
            tenant.phoneNumber != null && tenant.email != null ->
                "📞 ${tenant.phoneNumber}  •  ✉️ ${tenant.email}"
            tenant.phoneNumber != null -> "📞 ${tenant.phoneNumber}"
            tenant.email != null -> "✉️ ${tenant.email}"
            else -> "Nhà hàng OneOrder"
        }
    }

    // -------------------------
    // Posts
    // -------------------------

    fun refreshPosts() {
        val tenantId = _uiState.value.tenant?.id ?: return
        loadPosts(tenantId)
    }

    fun loadPosts(tenantId: String) {
        viewModelScope.launch {
            val result = foodPostRepository.getFoodPostsByTenant(tenantId)
            if (result.isSuccess) {
                _uiState.update { it.copy(posts = result.getOrNull() ?: emptyList()) }
            }
        }
    }

    fun togglePostActive(postId: String) {
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId }
            if (post != null) {
                val newActive = !post.isActive
                val result = foodPostRepository.updatePostActiveStatus(postId, newActive)
                if (result.isSuccess) {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { p ->
                                if (p.id == postId) p.copy(isActive = newActive) else p
                            }
                        )
                    }
                }
            }
        }
    }

    // (Removed add dialog show/hide methods since we use a separate screen)

    fun addPost(menuItemName: String, caption: String) {
        // Now handled by CreateFoodPostScreen
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = foodPostRepository.deleteFoodPost(postId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(posts = state.posts.filter { it.id != postId })
                }
            }
        }
    }

    fun setFeedStartIndex(index: Int) {
        _uiState.update { it.copy(feedStartIndex = index) }
    }

    // -------------------------
    // Likes & Comments
    // -------------------------

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId } ?: return@launch
            val newIsLiked = !post.isLiked
            val newLikeCount = if (newIsLiked) post.likeCount + 1 else post.likeCount - 1

            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { p ->
                        if (p.id == postId) p.copy(isLiked = newIsLiked, likeCount = newLikeCount) else p
                    }
                )
            }

            val result = foodPostRepository.toggleLike(postId, newIsLiked)
            if (result.isFailure) {
                // Revert if failed
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.map { p ->
                            if (p.id == postId) p.copy(isLiked = post.isLiked, likeCount = post.likeCount) else p
                        }
                    )
                }
            }
        }
    }

    fun openComments(postId: String) {
        _uiState.update { it.copy(showCommentSheet = true, commentsForPostId = postId, comments = emptyList()) }
        viewModelScope.launch {
            val result = foodPostRepository.getComments(postId)
            if (result.isSuccess) {
                _uiState.update { it.copy(comments = result.getOrNull() ?: emptyList()) }
            }
        }
    }

    fun closeComments() {
        _uiState.update { it.copy(showCommentSheet = false, commentsForPostId = null, commentInput = "") }
    }

    fun updateCommentInput(input: String) {
        _uiState.update { it.copy(commentInput = input) }
    }

    fun submitComment() {
        val postId = _uiState.value.commentsForPostId ?: return
        val content = _uiState.value.commentInput.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            val result = foodPostRepository.addComment(postId, content)
            if (result.isSuccess) {
                _uiState.update { it.copy(commentInput = "") }
                // Reload comments
                val commentsResult = foodPostRepository.getComments(postId)
                if (commentsResult.isSuccess) {
                    _uiState.update { state -> 
                        state.copy(
                            comments = commentsResult.getOrNull() ?: emptyList(),
                            posts = state.posts.map { p ->
                                if (p.id == postId) p.copy(commentCount = p.commentCount + 1) else p
                            }
                        )
                    }
                }
            }
        }
    }

    // -------------------------
    // Profile Edit (saves to DB)
    // -------------------------

    fun showEditProfileDialog() {
        _uiState.update { it.copy(showEditProfileDialog = true, editProfileError = null) }
    }

    fun hideEditProfileDialog() {
        _uiState.update { it.copy(showEditProfileDialog = false, editProfileError = null) }
    }

    fun updateProfile(id: String, name: String, address: String, phone: String, email: String, description: String, alignment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, editProfileError = null) }
            val finalDescription = if (description.isNotBlank()) "[$alignment]$description" else description

            val result = tenantRepository.updateTenant(
                name = name.takeIf { it.isNotBlank() },
                address = address.takeIf { it.isNotBlank() },
                phone = phone.takeIf { it.isNotBlank() },
                email = email.takeIf { it.isNotBlank() },
                description = finalDescription.takeIf { it.isNotBlank() }
            )
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isSavingProfile = false,
                        restaurantName = name,
                        restaurantId = id, // Update state with the new ID (even if not persisted yet)
                        description = description,
                        descriptionAlignment = alignment,
                        address = address,
                        phone = phone,
                        email = email,
                        bio = buildBioFromFields(phone, email),
                        showEditProfileDialog = false,
                        editProfileError = null,
                        successMessage = "Cập nhật thông tin nhà hàng thành công!"
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        editProfileError = "Lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    fun uploadRestaurantAvatar(imageBytes: ByteArray, extension: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, editProfileError = null) }
            val result = tenantRepository.uploadTenantLogo(imageBytes, extension)
            result.onSuccess { url ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        avatarUrl = url,
                        successMessage = "Cập nhật ảnh đại diện thành công!"
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        editProfileError = "Lỗi tải ảnh: ${e.message}"
                    )
                }
            }
        }
    }

    fun uploadRestaurantCover(imageBytes: ByteArray, extension: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, editProfileError = null) }
            val result = tenantRepository.uploadTenantCover(imageBytes, extension)
            result.onSuccess { url ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        coverUrl = url,
                        successMessage = "Cập nhật ảnh bìa thành công!"
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        editProfileError = "Lỗi tải ảnh bìa: ${e.message}"
                    )
                }
            }
        }
    }

    private fun buildBioFromFields(phone: String, email: String): String {
        return when {
            phone.isNotBlank() && email.isNotBlank() -> "📞 $phone  •  ✉️ $email"
            phone.isNotBlank() -> "📞 $phone"
            email.isNotBlank() -> "✉️ $email"
            else -> "Nhà hàng OneOrder"
        }
    }

    // -------------------------
    // Add Staff
    // -------------------------

    fun showAddStaffDialog() {
        _uiState.update { it.copy(showAddStaffDialog = true, addStaffError = null) }
    }

    fun hideAddStaffDialog() {
        _uiState.update { it.copy(showAddStaffDialog = false, addStaffError = null) }
    }

    fun createStaff(email: String, fullName: String, phone: String?, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, addStaffError = null) }
            val result = staffRepository.createStaffAccount(
                email = email,
                fullName = fullName,
                phone = phone,
                role = role
            )
            if (result.isSuccess) {
                val password = result.getOrNull()!!
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showAddStaffDialog = false,
                        tempPassword = password,
                        createdEmail = email
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        addStaffError = result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    )
                }
            }
        }
    }

    fun clearTempPassword() {
        _uiState.update { it.copy(tempPassword = null, createdEmail = null) }
    }
}

// Convert restaurant name to a URL-safe handle
private fun String.toHandle(): String {
    if (this.isEmpty()) return "nhahang"
    val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return normalized
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("đ", "d").replace("Đ", "D")
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
        .trim()
        .replace("\\s+".toRegex(), "_")
        .lowercase()
}
