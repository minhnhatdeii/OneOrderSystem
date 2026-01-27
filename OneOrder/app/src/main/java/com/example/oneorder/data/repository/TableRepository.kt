package com.example.oneorder.data.repository

import android.util.Log
import com.example.oneorder.data.model.TableInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class TableDto(
    val id: Long,
    val name: String,
    val capacity: Int? = null,
    val location: String? = null,
    val status: String = "free",
    @SerialName("qr_code_url")
    val qrCodeUrl: String? = null,
    @SerialName("tenant_id")
    val tenantId: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

interface TableRepository {
    suspend fun getTableById(tableId: Long): Result<TableInfo>
}

class TableRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TableRepository {
    
    override suspend fun getTableById(tableId: Long): Result<TableInfo> {
        return try {
            Log.d("TableRepository", "=== FETCHING TABLE ===")
            Log.d("TableRepository", "Table ID: $tableId")
            
            val table = supabase.postgrest.from("tables")
                .select {
                    filter { eq("id", tableId) }
                }
                .decodeSingleOrNull<TableDto>()
            
            if (table != null) {
                Log.d("TableRepository", "Table found: ${table.name}")
                Result.success(
                    TableInfo(
                        id = table.id,
                        name = table.name,
                        capacity = table.capacity,
                        location = table.location,
                        status = table.status
                    )
                )
            } else {
                Log.e("TableRepository", "Table not found")
                Result.failure(Exception("Table not found"))
            }
        } catch (e: Exception) {
            Log.e("TableRepository", "Error fetching table", e)
            Result.failure(e)
        }
    }
}
