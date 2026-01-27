package com.example.oneorder.ui.screens.qr

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.QRCodeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for QR Scanner screen
 */
@HiltViewModel
class QRScannerViewModel @Inject constructor(
    // We'll inject repositories if needed in the future
) : ViewModel() {

    private val _uiState = MutableStateFlow<QRScannerUiState>(QRScannerUiState.Idle)
    val uiState: StateFlow<QRScannerUiState> = _uiState.asStateFlow()

    /**
     * Process scanned QR code
     */
    fun onQRCodeScanned(rawValue: String) {
        viewModelScope.launch {
            _uiState.value = QRScannerUiState.Scanning
            
            // Log the raw QR code content
            Log.d("QRScannerViewModel", "=== QR CODE SCANNED ===")
            Log.d("QRScannerViewModel", "Raw QR Content: $rawValue")
            Log.d("QRScannerViewModel", "QR Content Length: ${rawValue.length}")
            Log.d("QRScannerViewModel", "First 100 chars: ${rawValue.take(100)}")
            
            try {
                val qrData = QRCodeData.fromString(rawValue)
                
                if (qrData != null) {
                    // Check if this is simplified format (restaurantId is empty marker)
                    if (qrData.restaurantId == QRCodeData.MARKER_CURRENT_RESTAURANT) {
                        Log.w("QRScannerViewModel", "=== SIMPLIFIED FORMAT DETECTED ===")
                        Log.w("QRScannerViewModel", "This QR code format is not supported")
                        Log.w("QRScannerViewModel", "Table ID: ${qrData.tableId}")
                        
                        _uiState.value = QRScannerUiState.Error(
                            "QR code lỗi thời!\n\n" +
                            "QR code này có định dạng cũ (oneorder://table/${qrData.tableId}) " +
                            "và không còn được hỗ trợ.\n\n" +
                            "Vui lòng yêu cầu nhà hàng tạo lại QR code mới từ ứng dụng OneOrder_SM.\n\n" +
                            "Định dạng mới: oneorder://restaurant/{id}/table/{id}"
                        )
                    } else {
                        // Full format - proceed normally
                        Log.d("QRScannerViewModel", "=== QR PARSING SUCCESS ===")
                        Log.d("QRScannerViewModel", "Restaurant ID: ${qrData.restaurantId}")
                        Log.d("QRScannerViewModel", "Table ID: ${qrData.tableId}")
                        _uiState.value = QRScannerUiState.Success(qrData)
                    }
                } else {
                    Log.e("QRScannerViewModel", "=== QR PARSING FAILED ===")
                    Log.e("QRScannerViewModel", "QR format is invalid")
                    Log.e("QRScannerViewModel", "Expected format: oneorder://restaurant/{id}/table/{id}")
                    Log.e("QRScannerViewModel", "Received: $rawValue")
                    _uiState.value = QRScannerUiState.Error("Mã QR không hợp lệ.\n\nĐịnh dạng mong đợi:\noneorder://restaurant/{id}/table/{id}\n\nĐã nhận:\n${rawValue.take(50)}...")
                }
            } catch (e: Exception) {
                Log.e("QRScannerViewModel", "=== QR PARSING EXCEPTION ===")
                Log.e("QRScannerViewModel", "Exception during QR parsing:", e)
                _uiState.value = QRScannerUiState.Error("Lỗi khi xử lý QR: ${e.message}")
            }
        }
    }

    /**
     * Handle permission denied
     */
    fun onPermissionDenied() {
        _uiState.value = QRScannerUiState.PermissionDenied
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _uiState.value = QRScannerUiState.Idle
    }
}
