package com.example.oneorder.data.repository

import android.util.Log
import com.example.oneorder.data.model.FeedImage
import com.example.oneorder.data.model.RestaurantProfilePost
import com.example.oneorder.data.model.RestaurantStats
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

interface RestaurantProfileRepository {
    suspend fun getRestaurantProfile(tenantId: String): Result<RestaurantProfileData>
    suspend fun getRestaurantPosts(tenantId: String): Result<List<RestaurantProfilePost>>
    suspend fun getSinglePostFull(tenantId: String, postId: String): Result<FoodPostDto>
    suspend fun getRestaurantPostsFullList(tenantId: String): Result<List<FoodPostDto>>
}

@Serializable
data class RestaurantProfileData(
    val id: String,
    @SerialName("tenant_id")
    val tenantId: String? = null,
    @SerialName("restaurant_name")
    val restaurantName: String = "",
    val address: String = "",
    val phone: String = "",
    val description: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val stats: RestaurantStats = RestaurantStats(0, 0, 0)
)

@Serializable
data class ProfileTenantDto(
    val id: String,
    @SerialName("owner_id")
    val ownerId: String? = null,
    @SerialName("restaurant_name")
    val restaurantName: String? = null,
    @SerialName("business_type")
    val businessType: String? = null,
    val address: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null
)

@Serializable
data class FoodPostDto(
    val id: String,
    val images: List<FeedImage>? = null,
    @SerialName("like_count")
    val likeCount: Int = 0,
    val caption: String? = null,
    @SerialName("menu_item_id")
    val menuItemId: Long? = null,
    @SerialName("tenant_id")
    val tenantId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable  
data class PostStatsDto(
    @SerialName("total_posts")
    val totalPosts: Long = 0,
    @SerialName("total_likes")
    val totalLikes: Long = 0
)

class RestaurantProfileRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : RestaurantProfileRepository {

    private val profileCache = mutableMapOf<String, RestaurantProfileData>()
    private val postsCache = mutableMapOf<String, List<RestaurantProfilePost>>()
    private val fullPostsCache = mutableMapOf<String, FoodPostDto>()
    private val fullPostsListCache = mutableMapOf<String, List<FoodPostDto>>()

    override suspend fun getRestaurantProfile(tenantId: String): Result<RestaurantProfileData> {
        profileCache[tenantId]?.let {
            Log.d("RestaurantProfileRepo", "Returning cached profile for tenant: $tenantId")
            return Result.success(it)
        }
        return try {
            withContext(Dispatchers.IO) {
                Log.d("RestaurantProfileRepo", "Fetching profile for tenant: $tenantId")

                // 1. Lấy thông tin từ bảng tenants
                val tenant = supabase.postgrest.from("tenants")
                    .select {
                        filter { eq("id", tenantId) }
                    }
                    .decodeSingleOrNull<ProfileTenantDto>()

                if (tenant == null) {
                    Log.w("RestaurantProfileRepo", "Tenant not found: $tenantId")
                    return@withContext Result.failure(Exception("Không tìm thấy thông tin nhà hàng"))
                }

                // 2. Tính stats từ food_posts
                val stats = getPostStats(tenantId)

                Log.d("RestaurantProfileRepo", "Found tenant: ${tenant.restaurantName}, posts: ${stats.totalPosts}, likes: ${stats.likes}")

                val profileData = RestaurantProfileData(
                    id = tenant.id,
                    tenantId = tenant.id,
                    restaurantName = tenant.restaurantName ?: "Nhà hàng",
                    address = tenant.address ?: "Chưa cập nhật",
                    phone = tenant.phoneNumber ?: "Chưa cập nhật",
                    description = tenant.businessType ?: "Chưa cập nhật",
                    avatarUrl = tenant.logoUrl,
                    coverUrl = null,
                    stats = stats
                )
                profileCache[tenantId] = profileData
                Result.success(profileData)
            }
        } catch (e: Exception) {
            Log.e("RestaurantProfileRepo", "Error fetching restaurant profile", e)
            Result.failure(e)
        }
    }

    override suspend fun getRestaurantPosts(tenantId: String): Result<List<RestaurantProfilePost>> {
        postsCache[tenantId]?.let {
            Log.d("RestaurantProfileRepo", "Returning cached posts for tenant: $tenantId")
            return Result.success(it)
        }
        return try {
            withContext(Dispatchers.IO) {
                Log.d("RestaurantProfileRepo", "Fetching posts for tenant: $tenantId")

                // Lấy posts từ food_posts
                val posts = supabase.postgrest.from("food_posts")
                    .select {
                        filter { eq("tenant_id", tenantId) }
                        order("created_at", order = Order.DESCENDING)
                        limit(20)
                    }
                    .decodeList<FoodPostDto>()

                val postList = posts.mapNotNull { post ->
                    try {
                        val imageUrl = extractFirstImageUrl(post.images)
                        if (imageUrl != null) {
                            fullPostsCache[post.id] = post
                            RestaurantProfilePost(
                                id = post.id,
                                imageUrl = imageUrl,
                                likeCount = post.likeCount
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.w("RestaurantProfileRepo", "Error parsing post ${post.id}", e)
                        null
                    }
                }

                fullPostsListCache[tenantId] = posts
                postsCache[tenantId] = postList
                Log.d("RestaurantProfileRepo", "Found ${postList.size} posts")
                Result.success(postList)
            }
        } catch (e: Exception) {
            Log.e("RestaurantProfileRepo", "Error fetching restaurant posts", e)
            Result.failure(e)
        }
    }

    override suspend fun getSinglePostFull(tenantId: String, postId: String): Result<FoodPostDto> {
        fullPostsCache[postId]?.let {
            return Result.success(it)
        }
        // Fallback: fetch from DB if not in cache
        return try {
            withContext(Dispatchers.IO) {
                val post = supabase.postgrest.from("food_posts")
                    .select { filter { eq("id", postId) } }
                    .decodeSingleOrNull<FoodPostDto>()
                if (post != null) {
                    fullPostsCache[postId] = post
                    Result.success(post)
                } else {
                    Result.failure(Exception("Post not found"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRestaurantPostsFullList(tenantId: String): Result<List<FoodPostDto>> {
        fullPostsListCache[tenantId]?.let {
            return Result.success(it)
        }
        // Fallback: fetch again if not cached
        return try {
            withContext(Dispatchers.IO) {
                val posts = supabase.postgrest.from("food_posts")
                    .select {
                        filter { eq("tenant_id", tenantId) }
                        order("created_at", order = Order.DESCENDING)
                        limit(20)
                    }
                    .decodeList<FoodPostDto>()
                
                fullPostsListCache[tenantId] = posts
                posts.forEach { fullPostsCache[it.id] = it }
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getPostStats(tenantId: String): RestaurantStats {
        return try {
            withContext(Dispatchers.IO) {
                // Đếm tổng posts và likes
                val posts = supabase.postgrest.from("food_posts")
                    .select {
                        filter { eq("tenant_id", tenantId) }
                    }
                    .decodeList<FoodPostDto>()

                val totalPosts = posts.size
                val totalLikes = posts.sumOf { it.likeCount.toLong() }
                
                // Tính followers dựa trên likes (mock logic)
                val followers = if (totalLikes > 0) {
                    (totalLikes * (0.3 + Math.random() * 0.7)).toInt()
                } else {
                    (100 + Math.random() * 5000).toInt()
                }

                RestaurantStats(
                    followers = followers.coerceAtLeast(1),
                    likes = totalLikes.toInt(),
                    totalPosts = totalPosts
                )
            }
        } catch (e: Exception) {
            Log.e("RestaurantProfileRepo", "Error calculating stats", e)
            RestaurantStats(followers = 100, likes = 0, totalPosts = 0)
        }
    }

    private fun extractFirstImageUrl(images: List<FeedImage>?): String? {
        return images?.firstOrNull()?.url
    }
}
