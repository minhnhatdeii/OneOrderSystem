package com.example.oneorder.data.model

import kotlinx.serialization.Serializable

/**
 * Data model representing information extracted from a QR code
 */
@Serializable
data class QRCodeData(
    val restaurantId: String,
    val tableId: Long
) {
    companion object {
        private const val TAG = "QRCodeData"
        const val MARKER_CURRENT_RESTAURANT = "" // Empty string marker for simplified format
        
        /**
         * Parse QR code content. Supports two formats:
         * 1. Full format: oneorder://restaurant/{restaurantId}/table/{tableId}
         * 2. Simplified format: oneorder://table/{tableId} (restaurantId will be empty string marker)
         */
        fun fromString(qrContent: String): QRCodeData? {
            android.util.Log.d(TAG, "=== PARSING QR CODE ===")
            android.util.Log.d(TAG, "Input: $qrContent")
            android.util.Log.d(TAG, "Length: ${qrContent.length}")
            
            return try {
                // Format 1: Full format - oneorder://restaurant/{restaurantId}/table/{tableId}
                if (qrContent.startsWith("oneorder://restaurant/")) {
                    android.util.Log.d(TAG, "Detected FULL format QR code")
                    
                    val parts = qrContent.removePrefix("oneorder://restaurant/").split("/table/")
                    android.util.Log.d(TAG, "Split into ${parts.size} parts")
                    
                    if (parts.size == 2) {
                        val restaurantId = parts[0]
                        val tableId = parts[1].toLongOrNull()
                        
                        android.util.Log.d(TAG, "Extracted restaurantId: $restaurantId")
                        android.util.Log.d(TAG, "Extracted tableId: $tableId")
                        
                        if (restaurantId.isNotEmpty() && tableId != null) {
                            android.util.Log.d(TAG, "=== PARSING SUCCESS (FULL FORMAT) ===")
                            return QRCodeData(restaurantId, tableId)
                        } else {
                            android.util.Log.w(TAG, "Invalid data: restaurantId isEmpty=${restaurantId.isEmpty()}, tableId is null=${tableId == null}")
                        }
                    } else {
                        android.util.Log.w(TAG, "Expected 2 parts after split, got ${parts.size}")
                        android.util.Log.w(TAG, "Parts: ${parts.joinToString()}")
                    }
                } 
                // Format 2: Simplified format - oneorder://table/{tableId}
                else if (qrContent.startsWith("oneorder://table/")) {
                    android.util.Log.d(TAG, "Detected SIMPLIFIED format QR code")
                    
                    val tableIdStr = qrContent.removePrefix("oneorder://table/")
                    val tableId = tableIdStr.toLongOrNull()
                    
                    android.util.Log.d(TAG, "Extracted tableId: $tableId")
                    
                    if (tableId != null) {
                        android.util.Log.d(TAG, "=== PARSING SUCCESS (SIMPLIFIED FORMAT) ===")
                        android.util.Log.d(TAG, "Restaurant ID will be resolved from user session")
                        return QRCodeData(MARKER_CURRENT_RESTAURANT, tableId)
                    } else {
                        android.util.Log.w(TAG, "Invalid tableId: $tableIdStr")
                    }
                } 
                else {
                    android.util.Log.w(TAG, "QR code does not match any supported format")
                    android.util.Log.w(TAG, "Expected: 'oneorder://restaurant/{id}/table/{id}' OR 'oneorder://table/{id}'")
                    android.util.Log.w(TAG, "Actual prefix: ${qrContent.take(30)}")
                }
                
                android.util.Log.w(TAG, "=== PARSING FAILED - RETURNING NULL ===")
                null
            } catch (e: Exception) {
                android.util.Log.e(TAG, "=== PARSING EXCEPTION ===", e)
                android.util.Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e(TAG, "Exception message: ${e.message}")
                null
            }
        }
    }
}

/**
 * Data model for restaurant information
 */
@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val address: String? = null
)
