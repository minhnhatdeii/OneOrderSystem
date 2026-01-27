package com.example.oneorder.ui.screens.qr

import android.Manifest
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onNavigateBack: () -> Unit,
    onScanSuccess: (String, Long) -> Unit, // restaurantId, tableId
    viewModel: QRScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()

    // Handle scan success
    LaunchedEffect(uiState) {
        Log.d("QRScanner", "=== UI STATE CHANGED ===")
        Log.d("QRScanner", "New state: ${uiState::class.simpleName}")
        
        if (uiState is QRScannerUiState.Success) {
            val qrData = (uiState as QRScannerUiState.Success).qrData
            Log.d("QRScanner", "=== SCAN SUCCESS - NAVIGATING ===")
            Log.d("QRScanner", "Restaurant ID: ${qrData.restaurantId}")
            Log.d("QRScanner", "Table ID: ${qrData.tableId}")
            onScanSuccess(qrData.restaurantId, qrData.tableId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    // Permission not granted - show request UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera permission is required to scan QR codes",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Grant Permission")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onNavigateBack) {
                            Text("Cancel")
                        }
                    }
                }
                uiState is QRScannerUiState.Error -> {
                    // Show error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = (uiState as QRScannerUiState.Error).message,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Try Again")
                        }
                    }
                }
                else -> {
                    // Show camera preview
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onQRCodeScanned = { rawValue ->
                            viewModel.onQRCodeScanned(rawValue)
                        }
                    )

                    // Scanner guide overlay
                    ScannerOverlay(
                        modifier = Modifier.fillMaxSize(),
                        isScanning = uiState is QRScannerUiState.Scanning
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // Track if we've already processed a QR code to avoid multiple scans
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            Log.d("QRScanner", "=== CAMERA PREVIEW INITIALIZATION ===")
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                Log.d("QRScanner", "Camera provider ready")
                val cameraProvider = cameraProviderFuture.get()
                
                // Preview use case
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                Log.d("QRScanner", "Preview use case created")

                // Image analysis use case for QR scanning
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                Log.d("QRScanner", "Image analysis use case created")

                val barcodeScanner = BarcodeScanning.getClient()
                Log.d("QRScanner", "Barcode scanner client initialized")

                var frameCount = 0
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    frameCount++
                    if (frameCount % 30 == 0) { // Log every 30 frames to avoid spam
                        Log.d("QRScanner", "Analyzing frame #$frameCount (hasScanned=$hasScanned)")
                    }
                    
                    if (!hasScanned) {
                        processImageProxy(barcodeScanner, imageProxy) { barcode ->
                            barcode.rawValue?.let { value ->
                                Log.d("QRScanner", "=== QR CODE DETECTED IN CAMERA ===")
                                Log.d("QRScanner", "Raw value: $value")
                                Log.d("QRScanner", "Barcode format: ${barcode.format}")
                                Log.d("QRScanner", "Value type: ${barcode.valueType}")
                                hasScanned = true
                                onQRCodeScanned(value)
                            }
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                // Camera selector
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.d("QRScanner", "Camera bound to lifecycle successfully")
                } catch (e: Exception) {
                    Log.e("QRScanner", "=== CAMERA BINDING FAILED ===", e)
                    Log.e("QRScanner", "Error message: ${e.message}")
                }
            }, executor)
            
            previewView
        },
        modifier = modifier
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (Barcode) -> Unit
) {
    imageProxy.image?.let { mediaImage ->
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    Log.d("QRScanner", "=== BARCODES DETECTED ===")
                    Log.d("QRScanner", "Number of barcodes found: ${barcodes.size}")
                    barcodes.forEachIndexed { index, barcode ->
                        Log.d("QRScanner", "Barcode #$index:")
                        Log.d("QRScanner", "  - Format: ${barcode.format}")
                        Log.d("QRScanner", "  - Value Type: ${barcode.valueType}")
                        Log.d("QRScanner", "  - Raw Value: ${barcode.rawValue}")
                        Log.d("QRScanner", "  - Display Value: ${barcode.displayValue}")
                    }
                }
                
                barcodes.firstOrNull()?.let { barcode ->
                    Log.d("QRScanner", "Processing first barcode...")
                    onBarcodeDetected(barcode)
                }
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "=== BARCODE SCANNING FAILED ===", e)
                Log.e("QRScanner", "Error type: ${e.javaClass.simpleName}")
                Log.e("QRScanner", "Error message: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } ?: run {
        Log.w("QRScanner", "ImageProxy.image is null, closing imageProxy")
        imageProxy.close()
    }
}

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    isScanning: Boolean = false
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Scanner frame
            Card(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(3.dp, if (isScanning) MaterialTheme.colorScheme.primary else Color.White)
            ) {}

            Spacer(modifier = Modifier.height(24.dp))

            // Instruction text
            Text(
                text = if (isScanning) "Processing..." else "Position QR code within the frame",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (isScanning) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}
