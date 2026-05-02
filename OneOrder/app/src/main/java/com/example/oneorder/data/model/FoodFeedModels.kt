package com.example.oneorder.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CardLayout {
    PORTRAIT,    // 4:5
    SQUARE,      // 1:1
    LANDSCAPE,   // 16:9
    TALL         // 3:4
}

@Serializable
data class FeedImage(
    val url: String,
    val layout: CardLayout = CardLayout.PORTRAIT
)

@Serializable
data class FeedPost(
    val id: String,
    @SerialName("tenant_id")
    val restaurantId: String? = null,
    @SerialName("restaurant_name")
    val restaurantName: String = "Restaurant Name",
    @SerialName("restaurant_address")
    val restaurantAddress: String = "Address",
    val restaurantLat: Double = 0.0,
    val restaurantLng: Double = 0.0,
    val distanceKm: Double = 0.0,
    val restaurantAvatar: String? = null,
    val menuItemName: String = "Item Name",
    val images: List<FeedImage> = emptyList(),
    val caption: String = "",
    val price: Double = 0.0,
    @SerialName("like_count")
    val likeCount: Int = 0,
    @SerialName("comment_count")
    val commentCount: Int = 0,
    @SerialName("share_count")
    val shareCount: Int = 0,
    var isLiked: Boolean = false,
    var isFollowing: Boolean = false
)

@Serializable
data class FoodComment(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("user_id")
    val userId: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class FeedCommentUI(
    val id: String,
    val userName: String,
    val avatarInitial: String,
    val content: String,
    val timeAgo: String
)

@Serializable
data class RestaurantStats(
    val followers: Int,
    val likes: Int,
    val totalPosts: Int
)

@Serializable
data class RestaurantProfilePost(
    val id: String,
    val imageUrl: String,
    val likeCount: Int
)
