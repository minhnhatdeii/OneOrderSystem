package com.example.oneorder_sm.ui.screens.foodpromotion

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicPaletteColors(
    val background: Color,
    val backgroundEnd: Color,
    val onBackground: Color,
    val accent: Color,
    val onAccent: Color
)

@Composable
fun rememberDominantPalette(
    imageUrl: String,
    isDark: Boolean,
    fallbackBackground: Color = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFAF9FF)
): DynamicPaletteColors {
    val context = LocalContext.current
    val fallbackScheme = buildFallbackColors(fallbackBackground, isDark)

    var paletteColors by remember(imageUrl, isDark) {
        mutableStateOf(fallbackScheme)
    }

    LaunchedEffect(imageUrl, isDark) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val req = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(128, 128)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(req)
                (result as? SuccessResult)?.drawable?.let { drawable ->
                    (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }
            } catch (_: Exception) {
                null
            }
        }

        bitmap?.let { bmp ->
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bmp)
                    .maximumColorCount(16)
                    .generate()
            }
            paletteColors = buildPaletteColors(palette, isDark)
        }
    }

    return paletteColors
}

private fun buildPaletteColors(palette: Palette, isDark: Boolean): DynamicPaletteColors {
    val primary = palette.darkVibrantSwatch
        ?: palette.vibrantSwatch
        ?: palette.darkMutedSwatch
        ?: palette.dominantSwatch

    val secondary = palette.lightVibrantSwatch
        ?: palette.lightMutedSwatch
        ?: palette.mutedSwatch

    val rawPrimary = primary?.rgb?.let { Color(it) }
    val rawSecondary = secondary?.rgb?.let { Color(it) }

    val background: Color
    val backgroundEnd: Color

    if (rawPrimary != null) {
        if (isDark) {
            background = rawPrimary.darkened(0.55f)
            backgroundEnd = rawPrimary.darkened(0.82f)
        } else {
            background = rawPrimary.lightened(0.88f)
            backgroundEnd = rawPrimary.lightened(0.96f)
        }
    } else {
        background = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFAF9FF)
        backgroundEnd = background
    }

    val onBackground = ensureContrast(background, isDark)

    val accent: Color
    val onAccent: Color
    if (rawSecondary != null) {
        accent = if (isDark) rawSecondary.darkened(0.4f) else rawSecondary.lightened(0.7f)
        onAccent = ensureContrast(accent, isDark)
    } else {
        accent = if (isDark) rawPrimary?.darkened(0.3f) ?: Color(0xFF3D3D3D)
        else rawPrimary?.lightened(0.75f) ?: Color(0xFFEEEEEE)
        onAccent = ensureContrast(accent, isDark)
    }

    return DynamicPaletteColors(
        background = background,
        backgroundEnd = backgroundEnd,
        onBackground = onBackground,
        accent = accent,
        onAccent = onAccent
    )
}

private fun buildFallbackColors(bg: Color, isDark: Boolean) = DynamicPaletteColors(
    background = bg,
    backgroundEnd = bg,
    onBackground = if (isDark) Color.White else Color(0xFF1C1B1F),
    accent = if (isDark) Color(0xFF3D3D3D) else Color(0xFFEEEEEE),
    onAccent = if (isDark) Color.White else Color(0xFF1C1B1F)
)

private fun ensureContrast(background: Color, preferDark: Boolean): Color {
    val lum = background.luminance()
    return if (lum > 0.35f) {
        Color(0xFF1A1A1A)
    } else {
        Color.White
    }
}

fun Color.darkened(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red   = red   * (1f - f),
        green = green * (1f - f),
        blue  = blue  * (1f - f),
        alpha = alpha
    )
}

fun Color.lightened(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red   = red   + (1f - red)   * f,
        green = green + (1f - green) * f,
        blue  = blue  + (1f - blue)  * f,
        alpha = alpha
    )
}
