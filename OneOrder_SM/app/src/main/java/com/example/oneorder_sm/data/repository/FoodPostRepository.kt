package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.data.model.ProfileWithTenant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.example.oneorder_sm.ui.screens.foodpromotion.PostImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FeedCommentUI(
    val id: String,
    val userName: String,
    val avatarInitial: String,
    val content: String,
    val timeAgo: String
)

@Serializable
data class FoodComment(
    val id: String,
    val post_id: String,
    val user_id: String,
    val content: String,
    val created_at: String
)

interface FoodPostRepository {
    suspend fun createFoodPost(
        menuItemId: Long,
        caption: String,
        tags: List<String>,
        imageUrls: List<String>
    ): Result<Unit>
    
    suspend fun uploadPostImage(byteArray: ByteArray, fileName: String): Result<String>
    suspend fun getFoodPostsByTenant(tenantId: String): Result<List<com.example.oneorder_sm.ui.screens.foodpromotion.FoodPost>>
    suspend fun deleteFoodPost(postId: String): Result<Unit>
    suspend fun updatePostActiveStatus(postId: String, isActive: Boolean): Result<Unit>
    suspend fun getComments(postId: String): Result<List<FeedCommentUI>>
    suspend fun addComment(postId: String, content: String): Result<Boolean>
    suspend fun toggleLike(postId: String, isLiked: Boolean): Result<Boolean>
}

@Serializable
data class FoodPostInsertDto(
    val tenant_id: String,
    val menu_item_id: Long,
    val caption: String,
    val category_tags: List<String>,
    val images: JsonElement
)

class FoodPostRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : FoodPostRepository {

    override suspend fun createFoodPost(
        menuItemId: Long,
        caption: String,
        tags: List<String>,
        imageUrls: List<String>
    ): Result<Unit> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val profile = supabase.postgrest.from("profiles")
                .select { filter { eq("id", currentUser.id) } }
                .decodeSingleOrNull<ProfileWithTenant>()
                
            val tenantId = profile?.tenantId
                ?: return Result.failure(Exception("Nhà hàng chưa được thiết lập."))

            // Build JSON array for images: [{"url": "http..."}, ...]
            val imagesJson = buildJsonArray {
                imageUrls.forEach { url ->
                    addJsonObject {
                        put("url", url)
                    }
                }
            }

            val post = FoodPostInsertDto(
                tenant_id = tenantId,
                menu_item_id = menuItemId,
                caption = caption,
                category_tags = tags,
                images = imagesJson
            )

            supabase.postgrest.from("food_posts").insert(post)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error creating food post", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadPostImage(byteArray: ByteArray, fileName: String): Result<String> {
        return try {
            if (byteArray.size > 10 * 1024 * 1024) {
                return Result.failure(Exception("Ảnh quá lớn (tối đa 10MB)."))
            }

            val bucket = supabase.storage.from("menu-images")
            bucket.upload("posts/$fileName", byteArray, upsert = true)
            val publicUrl = bucket.publicUrl("posts/$fileName")
            Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error uploading post image", e)
            Result.failure(e)
        }
    }

    override suspend fun getFoodPostsByTenant(tenantId: String): Result<List<com.example.oneorder_sm.ui.screens.foodpromotion.FoodPost>> {
        return try {
            // Because food_posts stores images as JSONB and references menu_items, we need to map it
            // We use select with inner join: "id, caption, like_count, comment_count, is_trending, created_at, images, menu_items(name, price, image_url, categories(name))"
            // The Supabase SDK can do nested selects if classes match, but we can also just fetch raw JSON and parse.
            val result = supabase.postgrest.from("food_posts")
                .select(Columns.raw("id, caption, like_count, comment_count, is_trending, created_at, images, menu_items(name, price, image_url, categories(name))")) {
                    filter { eq("tenant_id", tenantId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
            
            // To avoid complex nested serializable models, we parse JSON manually for now
            val jsonArray = result.decodeList<kotlinx.serialization.json.JsonObject>()
            val posts = jsonArray.map { obj ->
                val id = obj["id"]?.jsonPrimitive?.content ?: ""
                val caption = obj["caption"]?.jsonPrimitive?.content ?: ""
                val likeCount = obj["like_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val commentCount = obj["comment_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val isActive = true // We don't have is_active, let's just default to true
                
                // Format the date (simplified)
                val rawDate = obj["created_at"]?.jsonPrimitive?.content ?: ""
                val postedAt = rawDate.take(10) // YYYY-MM-DD
                
                // Images parsing - lấy tất cả ảnh
                val imagesArray = obj["images"]?.jsonArray
                val postImages = if (imagesArray != null && imagesArray.isNotEmpty()) {
                    imagesArray.mapNotNull { imgElement ->
                        val imgObj = imgElement.jsonObject
                        val url = imgObj["url"]?.jsonPrimitive?.content
                        if (url != null) {
                            PostImage(
                                url = url,
                                layout = imgObj["layout"]?.jsonPrimitive?.content ?: "PORTRAIT"
                            )
                        } else null
                    }
                } else emptyList()
                val firstImageUrl = postImages.firstOrNull()?.url

                // Menu item parsing
                val menuItem = obj["menu_items"]?.jsonObject
                val menuItemName = menuItem?.get("name")?.jsonPrimitive?.content ?: "Món ăn"
                val price = menuItem?.get("price")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val fallbackImage = menuItem?.get("image_url")?.jsonPrimitive?.content

                val category = menuItem?.get("categories")?.jsonObject
                val categoryName = category?.get("name")?.jsonPrimitive?.content ?: "Chưa phân loại"

                com.example.oneorder_sm.ui.screens.foodpromotion.FoodPost(
                    id = id,
                    menuItemName = menuItemName,
                    images = postImages.ifEmpty {
                        // Fallback: nếu không có ảnh từ food_posts, dùng ảnh từ menu_items
                        if (fallbackImage != null) listOf(PostImage(url = fallbackImage)) else emptyList()
                    },
                    caption = caption,
                    likeCount = likeCount,
                    commentCount = commentCount,
                    isActive = isActive,
                    postedAt = postedAt,
                    price = price,
                    categoryName = categoryName
                )
            }
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error getting food posts", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFoodPost(postId: String): Result<Unit> {
        return try {
            supabase.postgrest.from("food_posts").delete {
                filter { eq("id", postId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error deleting food post", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePostActiveStatus(postId: String, isActive: Boolean): Result<Unit> {
        return try {
            // we use is_trending to store active status as fallback if is_active is not available
            supabase.postgrest.from("food_posts").update(mapOf("is_trending" to isActive)) {
                filter { eq("id", postId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getComments(postId: String): Result<List<FeedCommentUI>> {
        return try {
            withContext(Dispatchers.IO) {
                val comments = supabase.postgrest.from("food_post_comments")
                    .select {
                        filter { eq("post_id", postId) }
                        order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<FoodComment>()

                // Map to UI representation
                val uiComments = comments.map {
                    FeedCommentUI(
                        id = it.id,
                        userName = "Quản lý",
                        avatarInitial = "Q",
                        content = it.content,
                        timeAgo = "Vừa xong"
                    )
                }
                Result.success(uiComments)
            }
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error getting comments", e)
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, content: String): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = supabase.auth.currentUserOrNull()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                supabase.postgrest.from("food_post_comments")
                    .insert(mapOf("post_id" to postId, "user_id" to currentUser.id, "content" to content))
                Result.success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error adding comment", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(postId: String, isLiked: Boolean): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = supabase.auth.currentUserOrNull()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                if (isLiked) {
                    supabase.postgrest.from("food_post_likes")
                        .insert(mapOf("post_id" to postId, "user_id" to currentUser.id))
                } else {
                    supabase.postgrest.from("food_post_likes")
                        .delete {
                            filter {
                                eq("post_id", postId)
                                eq("user_id", currentUser.id)
                            }
                        }
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("FoodPostRepo", "Error toggling like", e)
            Result.failure(e)
        }
    }
}
