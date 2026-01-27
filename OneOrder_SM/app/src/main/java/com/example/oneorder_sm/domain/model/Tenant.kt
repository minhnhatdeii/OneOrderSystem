package com.example.oneorder_sm.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a tenant (restaurant) in the multi-tenant system.
 * Each restaurant that registers on the platform gets their own tenant.
 */
@Serializable
data class Tenant(
    val id: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("restaurant_name")
    val restaurantName: String,
    @SerialName("business_type")
    val businessType: String = "restaurant",
    val address: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    val email: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    val timezone: String = "Asia/Ho_Chi_Minh",
    val currency: String = "VND",
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
