package com.example.oneorder_sm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Table(
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val id: Long? = null, // Nullable for auto-increment
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("tenant_id")
    val tenantId: String? = null,
    val name: String, // Changed from table_number to name
    val status: String = "free", // free, occupied
    val capacity: Int = 4, // Added capacity field
    val location: String? = null, // Location of table (e.g. "Tầng 1", "Sân vườn")
    @SerialName("qr_code_url")
    val qrCodeUrl: String? = null,
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("created_by")
    val createdBy: String? = null,
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

