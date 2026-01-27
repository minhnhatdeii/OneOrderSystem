package com.example.oneorder.data.repository

import com.example.oneorder.data.model.Category
import com.example.oneorder.data.model.MenuItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

interface MenuRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getMenuItems(categoryId: Long? = null): Result<List<MenuItem>>
    suspend fun getMenuItemsByCategory(categoryId: Long): Result<List<MenuItem>>
}

class MenuRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : MenuRepository {

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            val categories = supabase.postgrest.from("categories")
                .select().decodeList<Category>()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMenuItems(categoryId: Long?): Result<List<MenuItem>> {
        return try {
            val result = if (categoryId != null) {
                supabase.postgrest.from("menu_items").select {
                    filter {
                        eq("category_id", categoryId)
                    }
                }.decodeList<MenuItem>()
            } else {
                supabase.postgrest.from("menu_items").select().decodeList<MenuItem>() // Fetch all if no category
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getMenuItemsByCategory(categoryId: Long): Result<List<MenuItem>> {
        return try {
            val items = supabase.postgrest.from("menu_items").select {
                filter {
                    eq("category_id", categoryId)
                }
            }.decodeList<MenuItem>()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
