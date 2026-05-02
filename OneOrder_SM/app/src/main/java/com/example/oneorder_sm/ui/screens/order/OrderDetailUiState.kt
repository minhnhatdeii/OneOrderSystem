package com.example.oneorder_sm.ui.screens.order

import com.example.oneorder_sm.data.model.Order

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isSavingNote: Boolean = false
)
