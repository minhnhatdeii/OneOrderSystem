package com.example.oneorder.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long,
    val name: String,
    @SerialName("image_url")
    val image_url: String? = null,
    @SerialName("is_active")
    val is_active: Boolean = true,
    @SerialName("tenant_id")
    val tenant_id: String? = null,
    @SerialName("created_at")
    val created_at: String? = null,
    @SerialName("updated_at")
    val updated_at: String? = null,
    @SerialName("created_by")
    val created_by: String? = null
)

@Serializable
data class MenuItem(
    val id: Long,
    @SerialName("category_id")
    val category_id: Long,
    val name: String,
    val description: String? = null,
    val price: Double,
    @SerialName("image_url")
    val image_url: String? = null,
    @SerialName("is_available")
    val is_available: Boolean = true,
    @SerialName("tenant_id")
    val tenant_id: String? = null,
    @SerialName("created_at")
    val created_at: String? = null,
    @SerialName("updated_at")
    val updated_at: String? = null,
    @SerialName("created_by")
    val created_by: String? = null
)
