package com.example.oneorder_sm.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.example.oneorder_sm.data.model.Table
import com.example.oneorder_sm.data.repository.TableRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject

/**
 * Use case to get all tables
 */
class GetTablesUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(): Result<List<Table>> {
        return tableRepository.getTables()
    }
}

/**
 * Use case to add a new table
 */
class AddTableUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(table: Table): Result<Unit> {
        return tableRepository.addTable(table)
    }
}

/**
 * Use case to update a table
 */
class UpdateTableUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(table: Table): Result<Unit> {
        return tableRepository.updateTable(table)
    }
}

/**
 * Use case to delete a table
 */
class DeleteTableUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return tableRepository.deleteTable(id)
    }
}

/**
 * Use case to update table status
 */
class UpdateTableStatusUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(tableId: Long, status: String): Result<Unit> {
        return tableRepository.updateTableStatus(tableId, status)
    }
}

/**
 * Use case to generate QR code bitmap for a table
 * QR code contains deep link: oneorder://restaurant/{restaurantId}/table/{tableId}
 */
class GenerateQRCodeUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(tableId: Long, size: Int = 512): Result<Bitmap> {
        return try {
            android.util.Log.d("GenerateQRCodeUseCase", "=== GENERATING QR CODE ===")
            android.util.Log.d("GenerateQRCodeUseCase", "Table ID: $tableId")
            android.util.Log.d("GenerateQRCodeUseCase", "Size: $size")
            
            // Get table to retrieve tenant_id
            val tablesResult = tableRepository.getTables()
            if (tablesResult.isFailure) {
                android.util.Log.e("GenerateQRCodeUseCase", "Failed to get tables: ${tablesResult.exceptionOrNull()?.message}")
                return Result.failure(tablesResult.exceptionOrNull() ?: Exception("Failed to get tables"))
            }
            
            val allTables = tablesResult.getOrNull()
            android.util.Log.d("GenerateQRCodeUseCase", "Retrieved ${allTables?.size} tables from database")
            allTables?.forEach { t ->
                android.util.Log.d("GenerateQRCodeUseCase", "  Table: id=${t.id}, name=${t.name}, tenantId=${t.tenantId}")
            }
            
            val table = allTables?.find { it.id == tableId }
            if (table == null) {
                android.util.Log.e("GenerateQRCodeUseCase", "Table not found with ID: $tableId")
                return Result.failure(Exception("Table not found"))
            }
            
            android.util.Log.d("GenerateQRCodeUseCase", "Found table: $table")
            
            val restaurantId = table.tenantId
            if (restaurantId == null) {
                android.util.Log.e("GenerateQRCodeUseCase", "Table has no restaurant ID (tenantId is null)")
                android.util.Log.e("GenerateQRCodeUseCase", "Table details: id=${table.id}, name=${table.name}, tenantId=${table.tenantId}")
                return Result.failure(Exception("Table has no restaurant ID"))
            }
            
            android.util.Log.d("GenerateQRCodeUseCase", "Restaurant ID: $restaurantId")
            
            // Generate QR with proper format
            val content = "oneorder://restaurant/$restaurantId/table/$tableId"
            android.util.Log.d("GenerateQRCodeUseCase", "=== QR CONTENT ===")
            android.util.Log.d("GenerateQRCodeUseCase", "Generated content: $content")
            android.util.Log.d("GenerateQRCodeUseCase", "Content length: ${content.length}")
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            
            android.util.Log.d("GenerateQRCodeUseCase", "=== QR CODE GENERATED SUCCESSFULLY ===")
            android.util.Log.d("GenerateQRCodeUseCase", "Bitmap size: ${bitmap.width}x${bitmap.height}")
            Result.success(bitmap)
        } catch (e: Exception) {
            android.util.Log.e("GenerateQRCodeUseCase", "=== ERROR GENERATING QR CODE ===", e)
            android.util.Log.e("GenerateQRCodeUseCase", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("GenerateQRCodeUseCase", "Exception message: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
