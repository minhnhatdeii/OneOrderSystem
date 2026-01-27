package com.example.oneorder.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String, // UUID matches auth.users.id
    @SerialName("full_name") val fullName: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val role: String? = null, // Added to fix serialization error (e.g., "customer", "staff", etc.)
    @SerialName("tenant_id") val tenantId: String? = null, // Added for completeness
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
