package com.example.oneorder_sm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

@Serializable
data class Category(
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val id: Long? = null, // Change to nullable for auto-increment
    @SerialName("tenant_id")
    val tenantId: String? = null,
    val name: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class MenuItem(
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val id: Long? = null, // Change to nullable for auto-increment
    @kotlinx.serialization.ExperimentalSerializationApi
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    @SerialName("tenant_id")
    val tenantId: String? = null,
    @SerialName("category_id")
    val categoryId: Long,
    val name: String,
    @SerialName("description")
    val description: String? = null,
    val price: Double,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_available")
    val isAvailable: Boolean = true,
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

