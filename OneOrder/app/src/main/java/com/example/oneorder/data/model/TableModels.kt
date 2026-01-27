package com.example.oneorder.data.model

import kotlinx.serialization.Serializable

/**
 * Table information
 */
@Serializable
data class TableInfo(
    val id: Long,
    val name: String,
    val capacity: Int? = null,
    val location: String? = null,
    val status: String = "free"
)
