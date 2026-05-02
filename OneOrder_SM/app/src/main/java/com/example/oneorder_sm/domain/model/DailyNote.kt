package com.example.oneorder_sm.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DailyNote(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("staff_id")
    val staffId: String,
    @SerialName("tenant_id")
    val tenantId: String,
    @SerialName("note_date")
    val noteDate: String, // Format: YYYY-MM-DD
    val content: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_by")
    val createdBy: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("updated_at")
    val updatedAt: String? = null
)
