package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.data.model.Category
import com.example.oneorder_sm.data.model.MenuItem
import com.example.oneorder_sm.data.model.ProfileWithTenant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.upload
import io.github.jan.supabase.storage.storage
import javax.inject.Inject

interface MenuRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getMenuItems(categoryId: Long? = null): Result<List<MenuItem>>
    suspend fun createMenuItem(item: MenuItem): Result<Unit>
    suspend fun updateMenuItem(item: MenuItem): Result<Unit>
    suspend fun deleteMenuItem(id: Long): Result<Unit>
    suspend fun toggleItemAvailability(id: Long, isAvailable: Boolean): Result<Unit>
    suspend fun uploadImage(byteArray: ByteArray, fileName: String): Result<String> // Returns public URL
    suspend fun addCategory(category: Category, imageBytes: ByteArray?): Result<Unit>
    suspend fun updateCategory(category: Category, imageBytes: ByteArray?): Result<Unit>
    suspend fun deleteCategory(id: Long): Result<Unit>
}

class MenuRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : MenuRepository {



    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            val profile = supabase.postgrest.from("profiles")
                .select { filter { eq("id", currentUser.id) } }
                .decodeSingleOrNull<ProfileWithTenant>()
            val tenantId = profile?.tenantId
                ?: return Result.failure(Exception("Nhà hàng chưa được thiết lập."))

            val categories = supabase.postgrest.from("categories").select {
                filter { eq("tenant_id", tenantId) }
            }.decodeList<Category>()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMenuItems(categoryId: Long?): Result<List<MenuItem>> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            val profile = supabase.postgrest.from("profiles")
                .select { filter { eq("id", currentUser.id) } }
                .decodeSingleOrNull<ProfileWithTenant>()
            val tenantId = profile?.tenantId
                ?: return Result.failure(Exception("Nhà hàng chưa được thiết lập."))

            val items = supabase.postgrest.from("menu_items").select {
                filter {
                    eq("tenant_id", tenantId)
                    if (categoryId != null) {
                        eq("category_id", categoryId)
                    }
                }
            }.decodeList<MenuItem>()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createMenuItem(item: MenuItem): Result<Unit> {
        return try {
            // Get current user
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get tenant_id from profiles table (the correct source)
            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<ProfileWithTenant>()
            
            val tenantId = profile?.tenantId
            if (tenantId == null) {
                return Result.failure(Exception("Chưa có nhà hàng. Vui lòng đăng xuất và đăng nhập lại để hoàn tất thiết lập."))
            }
            
            val currentTimestamp = java.time.Instant.now().toString()
            
            
            // Create menu item with all required fields populated (id=null for auto-generation)
            val itemWithMetadata = item.copy(
                id = null, // Let database auto-generate
                tenantId = tenantId,
                createdBy = currentUser.id,
                createdAt = currentTimestamp,
                updatedAt = currentTimestamp
            )
            
            android.util.Log.d("MenuRepository", "Creating menu item: $itemWithMetadata")
            supabase.postgrest.from("menu_items").insert(itemWithMetadata)
            android.util.Log.d("MenuRepository", "Menu item created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MenuRepository", "Error creating menu item", e)
            Result.failure(e)
        }
    }

    override suspend fun updateMenuItem(item: MenuItem): Result<Unit> {
         return try {
            // Check if ID is not null for update
            if (item.id == null) {
                return Result.failure(Exception("Cannot update menu item without ID"))
            }
            
            val currentTimestamp = java.time.Instant.now().toString()
            
            // Only update mutable fields - exclude createdAt, createdBy, tenantId
            val itemToUpdate = item.copy(
                updatedAt = currentTimestamp,
                createdAt = null,  // Don't send in update
                createdBy = null,  // Don't send in update
                tenantId = null    // Don't send in update
            )
            
            supabase.postgrest.from("menu_items").update(itemToUpdate) {
                filter {
                    eq("id", item.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(byteArray: ByteArray, fileName: String): Result<String> {
        return try {
            android.util.Log.d("MenuRepository", "Uploading image: $fileName, size: ${byteArray.size}")

            // Check image size before upload
            if (byteArray.size > 10 * 1024 * 1024) { // 10MB limit
                return Result.failure(Exception("Image too large. Maximum size is 10MB. Current size: ${byteArray.size / (1024 * 1024)}MB"))
            }

            val bucket = supabase.storage.from("menu-images")

            // Check if bucket exists by trying to access it
            try {
                bucket.upload(fileName, byteArray, upsert = true)
                val publicUrl = bucket.publicUrl(fileName)
                android.util.Log.d("MenuRepository", "Image uploaded successfully: $publicUrl")
                Result.success(publicUrl)
            } catch (e: Exception) {
                android.util.Log.e("MenuRepository", "Bucket access error: ${e.message}", e)
                if (e.message?.contains("bucket") == true) {
                    Result.failure(Exception("Storage bucket 'menu-images' not found or not accessible. Please check Supabase storage configuration."))
                } else {
                    Result.failure(e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MenuRepository", "Error uploading image", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMenuItem(id: Long): Result<Unit> {
        return try {
            supabase.postgrest.from("menu_items").delete {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleItemAvailability(id: Long, isAvailable: Boolean): Result<Unit> {
        return try {
            supabase.postgrest.from("menu_items").update(
                { set("is_available", isAvailable) }
            ) {
                filter { eq("id", id) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCategory(category: Category, imageBytes: ByteArray?): Result<Unit> {
        return try {
            // Get current user
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            
            var imageUrl = category.imageUrl
            
            // Upload image if provided (though we're not using images for categories anymore)
            if (imageBytes != null) {
                val fileName = "category_${System.currentTimeMillis()}.jpg"
                val uploadResult = uploadImage(imageBytes, fileName)
                if (uploadResult.isFailure) {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
                }
                imageUrl = uploadResult.getOrNull()
            }

            // Get tenant_id from profiles table (the correct source)
            android.util.Log.d("MenuRepository", "Fetching tenant_id from profiles for user: ${currentUser.id}")
            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<ProfileWithTenant>()
            
            val tenantId = profile?.tenantId
            if (tenantId == null) {
                android.util.Log.e("MenuRepository", "User has no tenant_id in profile. Please complete restaurant setup first.")
                return Result.failure(Exception("Chưa có nhà hàng. Vui lòng đăng xuất và đăng nhập lại để hoàn tất thiết lập."))
            }
            
            android.util.Log.d("MenuRepository", "Using tenant_id: $tenantId")
            
            val currentTimestamp = java.time.Instant.now().toString()

            // Create category with all required fields populated (id=null for auto-generation)
            val categoryWithMetadata = category.copy(
                id = null, // Let database auto-generate
                imageUrl = imageUrl,
                tenantId = tenantId,
                createdBy = currentUser.id,
                createdAt = currentTimestamp,
                updatedAt = currentTimestamp
            )
            
            android.util.Log.d("MenuRepository", "Creating category: $categoryWithMetadata")
            supabase.postgrest.from("categories").insert(categoryWithMetadata)
            android.util.Log.d("MenuRepository", "Category created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MenuRepository", "Error saving category: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category, imageBytes: ByteArray?): Result<Unit> {
        return try {
            // Check if ID is not null for update
            if (category.id == null) {
                return Result.failure(Exception("Cannot update category without ID"))
            }

            var imageUrl = category.imageUrl

            // Upload new image if provided
            if (imageBytes != null) {
                val fileName = "category_${category.id}_${System.currentTimeMillis()}.jpg"
                val uploadResult = uploadImage(imageBytes, fileName)
                if (uploadResult.isFailure) {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
                }
                imageUrl = uploadResult.getOrNull()
            }

            val currentTimestamp = java.time.Instant.now().toString()
            
            // Update category with image URL and timestamp
            val categoryWithMetadata = category.copy(
                imageUrl = imageUrl,
                updatedAt = currentTimestamp
            )
            
            supabase.postgrest.from("categories").update(categoryWithMetadata) {
                filter {
                    eq("id", category.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(id: Long): Result<Unit> {
        return try {
            supabase.postgrest.from("categories").delete {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

