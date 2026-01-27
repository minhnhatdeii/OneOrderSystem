package com.example.oneorder_sm.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String?,
    val role: String, // 'customer', 'staff', 'manager'
    val avatar_url: String?
)
