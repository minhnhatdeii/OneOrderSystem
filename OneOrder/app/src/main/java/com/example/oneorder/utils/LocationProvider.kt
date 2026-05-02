package com.example.oneorder.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * LocationProvider — Lấy tọa độ GPS hiện tại của người dùng.
 *
 * Ưu tiên: FINE (GPS) → COARSE (Network/WiFi) → null nếu chưa cấp quyền.
 * Dùng FusedLocationProviderClient của Google Play Services để tiết kiệm pin
 * và cho kết quả nhanh hơn GPS thuần túy.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Trả về [Location] hiện tại hoặc null nếu chưa có quyền / không lấy được.
     *
     * Thử theo thứ tự:
     * 1. getCurrentLocation với priority BALANCED (nhanh, tiết kiệm pin)
     * 2. lastKnownLocation (instant, cached)
     * 3. null
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w("LocationProvider", "Chưa có quyền location — trả về null")
            // #region agent debug log
            Log.d("LocationProvider", ">>> [DEBUG] Permission denied")
            // #endregion
            return null
        }

        return try {
            // #region agent debug log
            Log.d("LocationProvider", ">>> [DEBUG] Getting current location...")
            // #endregion
            // Thử lấy vị trí hiện tại (max ~3 giây)
            val cts = CancellationTokenSource()
            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).addOnSuccessListener { loc ->
                    // #region agent debug log
                    Log.d("LocationProvider", ">>> [DEBUG] getCurrentLocation success: ${loc?.let { "${it.latitude},${it.longitude}" }}")
                    // #endregion
                    cont.resume(loc)
                }.addOnFailureListener { e ->
                    Log.e("LocationProvider", "getCurrentLocation failed", e)
                    cont.resume(null)
                }
                cont.invokeOnCancellation { cts.cancel() }
            }

            if (location != null) {
                Log.d("LocationProvider", "GPS: ${location.latitude}, ${location.longitude}")
                // #region agent debug log
                Log.d("LocationProvider", ">>> [DEBUG] Location not null, returning: ${location.latitude}, ${location.longitude}")
                // #endregion
                return location
            }

            // Fallback: lastKnownLocation (cached, instant)
            return suspendCancellableCoroutine { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            Log.d("LocationProvider", "LastKnown: ${loc.latitude}, ${loc.longitude}")
                            // #region agent debug log
                            Log.d("LocationProvider", ">>> [DEBUG] LastKnown location: ${loc.latitude}, ${loc.longitude}")
                            // #endregion
                        } else {
                            Log.w("LocationProvider", "lastKnownLocation cũng null, có thể GPS tắt")
                            // #region agent debug log
                            Log.d("LocationProvider", ">>> [DEBUG] LastKnown location null")
                            // #endregion
                        }
                        cont.resume(loc)
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationProvider", "lastKnownLocation failed", e)
                        cont.resume(null)
                    }
            }
        } catch (e: Exception) {
            Log.e("LocationProvider", "Lỗi khi lấy location", e)
            // #region agent debug log
            Log.d("LocationProvider", ">>> [DEBUG] Exception: ${e.message}")
            // #endregion
            null
        }
    }

    /**
     * Kiểm tra xem app đã được cấp quyền location chưa.
     * Trả về true nếu có ít nhất COARSE_LOCATION.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }
}
