package com.example.oneorder.data.repository

import android.util.Log
import com.example.oneorder.data.model.FeedCommentUI
import com.example.oneorder.data.model.FeedPost
import com.example.oneorder.data.model.FoodComment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

interface FoodFeedRepository {
    suspend fun getRecommendations(lat: Double, lng: Double, limit: Int = 10, offset: Int = 0, userId: String? = null): Result<List<FeedPost>>
    suspend fun getComments(postId: String): Result<List<FeedCommentUI>>
    suspend fun toggleLike(postId: String, isLiked: Boolean, userId: String): Result<Boolean>
    suspend fun addComment(postId: String, userId: String, content: String): Result<Boolean>
    suspend fun logInteraction(postId: String, userId: String, actionType: String, actionWeight: Double): Result<Boolean>
    suspend fun checkUserProfileExists(userId: String): Boolean
    suspend fun saveInitialUserProfile(userId: String, selectedTags: List<Pair<String, String>>): Result<Boolean>
}

@Serializable
data class FeedRequest(
    val lat: Double,
    val lng: Double,
    val page: Int,
    val limit: Int,
    val refresh: Boolean
)

@Serializable
data class FeedResponse(
    val data: List<FeedPost>,
    val source: String
)

@Serializable
data class InteractionLogEntry(
    @SerialName("post_id")
    val postId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("action_type")
    val actionType: String,
    @SerialName("action_weight")
    val actionWeight: Double
)

class FoodFeedRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : FoodFeedRepository {

    override suspend fun getRecommendations(lat: Double, lng: Double, limit: Int, offset: Int, userId: String?): Result<List<FeedPost>> {
        return try {
            withContext(Dispatchers.IO) {
                val page = (offset / limit) + 1
                val isRefresh = (offset == 0)

                // #region agent debug log
                Log.d("FoodFeedRepo", ">>> [DEBUG] Calling Edge Function: lat=$lat, lng=$lng, page=$page, limit=$limit, offset=$offset, refresh=$isRefresh")
                // #endregion

                val request = FeedRequest(
                    lat = lat,
                    lng = lng,
                    page = page,
                    limit = limit,
                    refresh = isRefresh
                )

                // Gọi Edge Function get-food-feed
                val response = supabase.functions.invoke(
                    function = "get-food-feed",
                    body = request
                )

                // #region agent debug log
                Log.d("FoodFeedRepo", ">>> [DEBUG] Edge Function response status:")
                // #endregion

                // ── FIX: Kiểm tra HTTP status và error body trước khi deserialize ──
                // Edge Function trả về HTTP 200 nhưng body chứa {"error": "..."}
                // Nếu không check, body<FeedResponse> sẽ throw exception → UnauthorizedRestException
                val rawBody = response.bodyAsText()
                if (rawBody.contains("\"error\"")) {
                    // Trích error message từ JSON: {"error": "message"}
                    val errorMsg = try {
                        rawBody.substringAfter("\"error\":\"").substringBefore("\"").ifEmpty {
                            rawBody.substringAfter("\"error\":").substringBefore("}").trim()
                        }
                    } catch (_: Exception) {
                        rawBody
                    }
                    // #region agent debug log
                    Log.d("FoodFeedRepo", ">>> [DEBUG] Edge Function returned error: $errorMsg")
                    // #endregion
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val result = response.body<FeedResponse>()

                // #region agent debug log
                Log.d("FoodFeedRepo", ">>> [DEBUG] Edge Function success: postsCount=${result.data.size}, source=${result.source}")
                // #endregion
                Result.success(result.data)
            }
        } catch (e: Exception) {
            // #region agent debug log
            Log.e("FoodFeedRepo", ">>> [DEBUG] Repository exception: ${e.message}, type=${e.javaClass.simpleName}")
            // #endregion
            Log.e("FoodFeedRepo", "Error getting recommendations", e)
            Result.failure(e)
        }
    }

    override suspend fun getComments(postId: String): Result<List<FeedCommentUI>> {
        return try {
            withContext(Dispatchers.IO) {
                val comments = supabase.postgrest.from("food_post_comments")
                    .select {
                        filter { eq("post_id", postId) }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<FoodComment>()

                // Map to UI representation
                val uiComments = comments.map {
                    FeedCommentUI(
                        id = it.id,
                        userName = "User", // Would require joining users table
                        avatarInitial = "U",
                        content = it.content,
                        timeAgo = "Vừa xong"
                    )
                }
                Result.success(uiComments)
            }
        } catch (e: Exception) {
            Log.e("FoodFeedRepo", "Error getting comments", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(postId: String, isLiked: Boolean, userId: String): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                if (isLiked) {
                    supabase.postgrest.from("food_post_likes")
                        .insert(mapOf("post_id" to postId, "user_id" to userId))
                } else {
                    supabase.postgrest.from("food_post_likes")
                        .delete {
                            filter {
                                eq("post_id", postId)
                                eq("user_id", userId)
                            }
                        }
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FoodFeedRepo", "Error toggling like", e)
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, userId: String, content: String): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                supabase.postgrest.from("food_post_comments")
                    .insert(mapOf("post_id" to postId, "user_id" to userId, "content" to content))
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FoodFeedRepo", "Error adding comment", e)
            Result.failure(e)
        }
    }

    override suspend fun logInteraction(postId: String, userId: String, actionType: String, actionWeight: Double): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val payload = InteractionLogEntry(
                    postId = postId,
                    userId = userId,
                    actionType = actionType,
                    actionWeight = actionWeight
                )
                supabase.postgrest.from("interactions_log").insert(payload)
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FoodFeedRepo", "Error logging interaction", e)
            Result.failure(e)
        }
    }

    override suspend fun checkUserProfileExists(userId: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val profile = supabase.postgrest.from("user_profiles")
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()
                profile != null
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveInitialUserProfile(userId: String, selectedTags: List<Pair<String, String>>): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                // selectedTags contains pairs like ("cuisine", "vietnamese"), ("flavor", "spicy")
                // We need to build: { "cuisine": { "vietnamese": 3.0 }, "flavor": { "spicy": 3.0 } }
                
                val tagProfile = buildJsonObject {
                    val grouped = selectedTags.groupBy { it.first }
                    grouped.forEach { (category, tags) ->
                        put(category, buildJsonObject {
                            tags.forEach { tag ->
                                put(tag.second, 3.0) // Give initial weight of 3.0
                            }
                        })
                    }
                }

                val profileData = buildJsonObject {
                    put("user_id", userId)
                    put("tag_profile", tagProfile)
                }

                supabase.postgrest.from("user_profiles").upsert(profileData)
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FoodFeedRepo", "Error saving initial profile", e)
            Result.failure(e)
        }
    }
}
