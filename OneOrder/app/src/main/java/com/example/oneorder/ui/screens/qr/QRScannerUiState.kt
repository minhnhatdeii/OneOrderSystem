package com.example.oneorder.ui.screens.qr

import com.example.oneorder.data.model.QRCodeData

/**
 * UI state for QR Scanner screen
 */
sealed interface QRScannerUiState {
    data object Idle : QRScannerUiState
    data object Scanning : QRScannerUiState
    data class Success(val qrData: QRCodeData) : QRScannerUiState
    data class Error(val message: String) : QRScannerUiState
    data object PermissionDenied : QRScannerUiState
}
