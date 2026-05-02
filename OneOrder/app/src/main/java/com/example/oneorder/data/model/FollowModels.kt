package com.example.oneorder.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FollowedRestaurant(
    val followId: String,
    val tenantId: String,
    val restaurantName: String,
    val avatarUrl: String?,
    val coverUrl: String?,
    val address: String?,
    val description: String?,
    val followersCount: Int,
    val totalPosts: Int,
    val createdAt: String
)
