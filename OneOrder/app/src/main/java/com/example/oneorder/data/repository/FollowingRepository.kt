package com.example.oneorder.data.repository

import android.util.Log
import com.example.oneorder.data.model.FollowedRestaurant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

interface FollowingRepository {
    suspend fun getFollowedRestaurants(): Result<List<FollowedRestaurant>>
    suspend fun followRestaurant(tenantId: String): Result<Unit>
    suspend fun unfollowRestaurant(tenantId: String): Result<Unit>
    suspend fun isFollowing(tenantId: String): Result<Boolean>
    suspend fun getFollowingCount(): Result<Int>
    fun getFollowingCountFlow(): StateFlow<Int>
    fun getFollowedTenantIdsFlow(): StateFlow<Set<String>>
    fun invalidateCache()
}

@Serializable
private data class FollowRecord(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class FollowedRestaurantDto(
    val followId: String,
    @SerialName("tenant_id")
    val tenantId: String,
    @SerialName("restaurant_name")
    val restaurantName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val address: String? = null,
    val description: String? = null,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("total_posts")
    val totalPosts: Int = 0,
    @SerialName("created_at")
    val createdAt: String = ""
)

@Singleton
class FollowingRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : FollowingRepository {

    private var cachedList: List<FollowedRestaurant>? = null

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _followingCount = MutableStateFlow(0)
    private val _followingCountFlow = _followingCount.asStateFlow()

    // Set of tenant IDs the current user follows — updated on every invalidation
    private val _followedTenantIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        repoScope.launch {
            refreshFollowData()
        }
        initRealtime()
    }

    private var realtimeChannel: RealtimeChannel? = null

    private fun initRealtime() {
        val channel = supabase.realtime.channel("followed_restaurants_changes")
        realtimeChannel = channel

        // Collect Postgres changes (INSERT / UPDATE / DELETE) on the followed_restaurants table
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(
            schema = "public"
        ) {
            table = "followed_restaurants"
        }

        repoScope.launch {
            changeFlow.collect {
                Log.d("FollowingRepo", "Realtime change received, refreshing follow data")
                refreshFollowData()
            }
        }

        repoScope.launch {
            channel.subscribe()
        }
    }

    private fun cleanupRealtime() {
        realtimeChannel?.let {
            repoScope.launch {
                supabase.realtime.removeChannel(it)
            }
            realtimeChannel = null
        }
    }

    override fun getFollowingCountFlow(): StateFlow<Int> = _followingCountFlow
    override fun getFollowedTenantIdsFlow(): StateFlow<Set<String>> = _followedTenantIds

    private fun updateCount(count: Int) {
        _followingCount.value = count
    }

    /** Fetches the full list of followed tenant IDs and count in one query */
    private suspend fun refreshFollowData() {
        try {
            withContext(Dispatchers.IO) {
                val rows = supabase.postgrest.from("followed_restaurants")
                    .select()
                    .decodeList<FollowRecord>()
                _followingCount.value = rows.size
                _followedTenantIds.value = rows.map { it.tenantId }.toSet()
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "refreshFollowData failed", e)
        }
    }

    override suspend fun getFollowedRestaurants(): Result<List<FollowedRestaurant>> {
        return try {
            withContext(Dispatchers.IO) {
                Log.d("FollowingRepo", "Fetching followed restaurants")

                val response = supabase.postgrest.rpc("get_followed_restaurants")
                    .decodeList<FollowedRestaurantDto>()

                val list = response.map { dto ->
                    FollowedRestaurant(
                        followId = dto.followId,
                        tenantId = dto.tenantId,
                        restaurantName = dto.restaurantName,
                        avatarUrl = dto.avatarUrl,
                        coverUrl = dto.coverUrl,
                        address = dto.address,
                        description = dto.description,
                        followersCount = dto.followersCount,
                        totalPosts = dto.totalPosts,
                        createdAt = dto.createdAt
                    )
                }

                cachedList = list
                updateCount(list.size)
                Log.d("FollowingRepo", "Found ${list.size} followed restaurants")
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error fetching followed restaurants", e)
            // Fallback: try direct table query if RPC doesn't exist yet
            fallbackGetFollowedRestaurants()
        }
    }

    private suspend fun fallbackGetFollowedRestaurants(): Result<List<FollowedRestaurant>> {
        return try {
            withContext(Dispatchers.IO) {
                @Serializable
                data class TenantInfo(
                    val id: String,
                    @SerialName("restaurant_name")
                    val restaurantName: String? = null,
                    val name: String? = null,
                    val address: String? = null,
                    @SerialName("logo_url")
                    val logoUrl: String? = null,
                    @SerialName("avatar_url")
                    val avatarUrl: String? = null
                )

                val follows = supabase.postgrest.from("followed_restaurants")
                    .select()
                    .decodeList<FollowRecord>()

                val result = follows.mapNotNull { follow ->
                    try {
                        val tenant = supabase.postgrest.from("tenants")
                            .select { filter { eq("id", follow.tenantId) } }
                            .decodeSingleOrNull<TenantInfo>()

                        if (tenant != null) {
                            FollowedRestaurant(
                                followId = follow.id,
                                tenantId = follow.tenantId,
                                restaurantName = tenant.restaurantName ?: tenant.name ?: "Nhà hàng",
                                avatarUrl = tenant.logoUrl ?: tenant.avatarUrl,
                                coverUrl = null,
                                address = tenant.address,
                                description = null,
                                followersCount = 0,
                                totalPosts = 0,
                                createdAt = follow.createdAt
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }

                cachedList = result
                updateCount(result.size)
                Result.success(result)
            }
        } catch (e: Exception) {
            // Table doesn't exist yet - return empty list gracefully
            cachedList = emptyList()
            updateCount(0)
            Result.success(emptyList())
        }
    }

    override suspend fun followRestaurant(tenantId: String): Result<Unit> {
        // Optimistic update: add immediately so UI reacts at once
        _followedTenantIds.value = _followedTenantIds.value + tenantId
        _followingCount.value = _followedTenantIds.value.size
        return try {
            withContext(Dispatchers.IO) {
                Log.d("FollowingRepo", "Following restaurant: $tenantId")
                supabase.postgrest.rpc("follow_restaurant", mapOf("p_tenant_id" to tenantId))
                invalidateCache()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error following restaurant", e)
            // Fallback: direct insert
            fallbackFollowRestaurant(tenantId)
        }
    }

    private suspend fun fallbackFollowRestaurant(tenantId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                supabase.postgrest.from("followed_restaurants").insert(
                    mapOf("tenant_id" to tenantId)
                )
                invalidateCache()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Fallback follow failed", e)
            // Revert optimistic update on failure
            _followedTenantIds.value = _followedTenantIds.value - tenantId
            _followingCount.value = _followedTenantIds.value.size
            Result.failure(e)
        }
    }

    override suspend fun unfollowRestaurant(tenantId: String): Result<Unit> {
        // Optimistic update: remove immediately so UI reacts at once
        _followedTenantIds.value = _followedTenantIds.value - tenantId
        _followingCount.value = _followedTenantIds.value.size
        return try {
            withContext(Dispatchers.IO) {
                Log.d("FollowingRepo", "Unfollowing restaurant: $tenantId")
                supabase.postgrest.rpc("unfollow_restaurant", mapOf("p_tenant_id" to tenantId))
                invalidateCache()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error unfollowing restaurant", e)
            // Fallback: direct delete
            fallbackUnfollowRestaurant(tenantId)
        }
    }

    private suspend fun fallbackUnfollowRestaurant(tenantId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                supabase.postgrest.from("followed_restaurants")
                    .delete { filter { eq("tenant_id", tenantId) } }
                invalidateCache()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Fallback unfollow failed", e)
            // Revert optimistic update on failure
            _followedTenantIds.value = _followedTenantIds.value + tenantId
            _followingCount.value = _followedTenantIds.value.size
            Result.failure(e)
        }
    }

    override suspend fun isFollowing(tenantId: String): Result<Boolean> {
        // Fast path: check in-memory set first
        if (_followedTenantIds.value.isNotEmpty()) {
            return Result.success(_followedTenantIds.value.contains(tenantId))
        }
        return try {
            withContext(Dispatchers.IO) {
                Log.d("FollowingRepo", "Checking follow status via RPC: $tenantId")
                val result = supabase.postgrest.rpc(
                    "is_following_restaurant",
                    mapOf("p_tenant_id" to tenantId)
                ).decodeAs<Boolean>()
                // Also update our local set if we got a positive result
                if (result) {
                    _followedTenantIds.value = _followedTenantIds.value + tenantId
                }
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "RPC isFollowing failed, trying direct query", e)
            try {
                withContext(Dispatchers.IO) {
                    val rows = supabase.postgrest.from("followed_restaurants")
                        .select { filter { eq("tenant_id", tenantId) } }
                        .decodeList<FollowRecord>()
                    val following = rows.isNotEmpty()
                    if (following) {
                        _followedTenantIds.value = _followedTenantIds.value + tenantId
                    }
                    Result.success(following)
                }
            } catch (fallbackEx: Exception) {
                Log.e("FollowingRepo", "Direct query isFollowing failed", fallbackEx)
                Result.success(_followedTenantIds.value.contains(tenantId))
            }
        }
    }

    override suspend fun getFollowingCount(): Result<Int> {
        return try {
            withContext(Dispatchers.IO) {
                val count = supabase.postgrest.from("followed_restaurants")
                    .select()
                    .decodeList<FollowRecord>()
                    .size
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.success(0)
        }
    }

    override fun invalidateCache() {
        cachedList = null
        repoScope.launch {
            refreshFollowData()
        }
    }
}
