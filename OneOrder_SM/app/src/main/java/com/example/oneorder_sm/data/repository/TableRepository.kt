package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.data.model.ProfileWithTenant
import com.example.oneorder_sm.data.model.Table
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

interface TableRepository {
    suspend fun getTables(): Result<List<Table>>
    suspend fun addTable(table: Table): Result<Unit>
    suspend fun updateTable(table: Table): Result<Unit>
    suspend fun deleteTable(id: Long): Result<Unit>
    suspend fun updateTableStatus(tableId: Long, status: String): Result<Unit>
}

class TableRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TableRepository {

    override suspend fun getTables(): Result<List<Table>> {
        return try {
            android.util.Log.d("TableRepository", "=== FETCHING TABLES ===")
            
            // Select with asterisk to get all columns including tenant_id
            val tables = supabase.postgrest.from("tables")
                .select()
                .decodeList<Table>()
            
            android.util.Log.d("TableRepository", "Retrieved ${tables.size} tables")
            tables.forEachIndexed { index, table ->
                android.util.Log.d("TableRepository", "Table #$index: id=${table.id}, name=${table.name}, tenantId='${table.tenantId}'")
                if (table.tenantId == null) {
                    android.util.Log.w("TableRepository", "⚠️ Table #$index has NULL tenant_id!")
                }
            }
            
            Result.success(tables.sortedBy { it.id })
        } catch (e: Exception) {
            android.util.Log.e("TableRepository", "Error fetching tables", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTableStatus(tableId: Long, status: String): Result<Unit> {
        return try {
            supabase.postgrest.from("tables").update(
                { set("status", status) }
            ) {
                filter { eq("id", tableId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTable(table: Table): Result<Unit> {
        return try {
            android.util.Log.d("TableRepository", "Adding table: $table")
            
            // Get current user
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get tenant_id from profiles table
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
            
            // Create table with all required fields populated
            val tableWithMetadata = table.copy(
                id = null, // Let database auto-generate
                tenantId = tenantId,
                createdBy = currentUser.id,
                createdAt = currentTimestamp,
                updatedAt = currentTimestamp
            )
            
            android.util.Log.d("TableRepository", "Inserting table with metadata: $tableWithMetadata")
            supabase.postgrest.from("tables").insert(tableWithMetadata)
            android.util.Log.d("TableRepository", "Table added successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TableRepository", "Error adding table", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTable(table: Table): Result<Unit> {
        return try {
            if (table.id == null) {
                return Result.failure(Exception("Cannot update table without ID"))
            }
            
            val currentTimestamp = java.time.Instant.now().toString()
            
            // Only update mutable fields - exclude createdAt, createdBy, tenantId
            val tableToUpdate = table.copy(
                updatedAt = currentTimestamp,
                createdAt = null,  // Don't send in update
                createdBy = null,  // Don't send in update
                tenantId = null    // Don't send in update
            )
            
            supabase.postgrest.from("tables").update(tableToUpdate) {
                filter {
                    eq("id", table.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTable(id: Long): Result<Unit> {
        return try {
            supabase.postgrest.from("tables").delete {
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

