package com.example.oneorder_sm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Simple data class for decoding profile with tenant_id from database
 * Used internally by repositories to fetch tenant information
 */
@Serializable
internal data class ProfileWithTenant(
    val id: String,
    @SerialName("tenant_id")
    val tenantId: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    val role: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null
)
