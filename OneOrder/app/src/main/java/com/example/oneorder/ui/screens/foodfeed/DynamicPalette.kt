package com.example.oneorder.ui.screens.foodfeed

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

// ═══════════════════════════════════════════════════════════════
//  DynamicPaletteColors — bộ màu hoàn chỉnh từ 1 ảnh
// ═══════════════════════════════════════════════════════════════

/**
 * Holds all computed colors for a single feed card.
 *
 * @param background   Top gradient color (dominant / vibrant)
 * @param backgroundEnd Bottom gradient color (darker/lighter variant)
 * @param onBackground Text / icon color that contrasts with [background]
 * @param accent       Secondary accent (e.g., price badge) — LightVibrant or Muted
 * @param onAccent     Text color on [accent]
 */
data class DynamicPaletteColors(
    val background: Color,
    val backgroundEnd: Color,
    val onBackground: Color,
    val accent: Color,
    val onAccent: Color
)

// ═══════════════════════════════════════════════════════════════
//  rememberDominantPalette — Composable State holder
// ═══════════════════════════════════════════════════════════════

/**
 * Returns [DynamicPaletteColors] derived from [imageUrl].
 *
 * - Loads a downsampled 64×64 bitmap via Coil (fast, cached).
 * - Runs Palette generation on IO thread.
 * - Falls back to [fallbackBackground] if extraction fails.
 * - Applies luminance-based contrast rules so text is always legible.
 *
 * @param imageUrl          URL of the food image to sample.
 * @param isDark            Whether the system is currently in dark mode.
 * @param fallbackBackground Default background if Palette extraction fails.
 */
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
                    // Small thumbnail is enough for color extraction — fast & cheap
                    .size(128, 128)
                    .allowHardware(false)   // Palette needs software bitmap
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

// ═══════════════════════════════════════════════════════════════
//  Internal helpers
// ═══════════════════════════════════════════════════════════════

/**
 * Converts Palette swatches into [DynamicPaletteColors], tuned for
 * readability in both dark and light modes.
 */
private fun buildPaletteColors(palette: Palette, isDark: Boolean): DynamicPaletteColors {
    // Priority: DarkVibrant > Vibrant > DarkMuted > Dominant
    val primary = palette.darkVibrantSwatch
        ?: palette.vibrantSwatch
        ?: palette.darkMutedSwatch
        ?: palette.dominantSwatch

    // Accent: LightVibrant > LightMuted (complementary to primary)
    val secondary = palette.lightVibrantSwatch
        ?: palette.lightMutedSwatch
        ?: palette.mutedSwatch

    val rawPrimary = primary?.rgb?.let { Color(it) }
    val rawSecondary = secondary?.rgb?.let { Color(it) }

    // In DARK mode → use darker, more saturated variant
    // In LIGHT mode → lighten + desaturate slightly so text stays dark
    val background: Color
    val backgroundEnd: Color

    if (rawPrimary != null) {
        if (isDark) {
            background = rawPrimary.darkened(0.55f)   // keep moody
            backgroundEnd = rawPrimary.darkened(0.82f) // almost black at bottom
        } else {
            background = rawPrimary.lightened(0.88f)  // very light tint
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
        // fallback: use primary tint
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

// ── Color math ────────────────────────────────────────────────

/**
 * Returns a text color (white or very-dark) that contrasts well
 * against [background]. Uses WCAG-based luminance threshold.
 */
private fun ensureContrast(background: Color, preferDark: Boolean): Color {
    val lum = background.luminance()
    // Switch at 0.35 so we stay readable across wide palette range
    return if (lum > 0.35f) {
        // Light background → need dark text
        Color(0xFF1A1A1A)
    } else {
        // Dark background → need light text
        Color.White
    }
}

/**
 * Darkens a [Color] by blending toward black.
 * @param factor 0f = original, 1f = black
 */
fun Color.darkened(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red   = red   * (1f - f),
        green = green * (1f - f),
        blue  = blue  * (1f - f),
        alpha = alpha
    )
}

/**
 * Lightens a [Color] by blending toward white.
 * @param factor 0f = original, 1f = white
 */
fun Color.lightened(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red   = red   + (1f - red)   * f,
        green = green + (1f - green) * f,
        blue  = blue  + (1f - blue)  * f,
        alpha = alpha
    )
}
