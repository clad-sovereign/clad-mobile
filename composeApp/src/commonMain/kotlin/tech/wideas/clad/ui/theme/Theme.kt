package tech.wideas.clad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * CLAD Sovereign Institutional Theme
 *
 * Uses Material You's Tonal Palette system (via Material Kolor KMP library)
 * to generate perceptually uniform color schemes from dark brand colors.
 *
 * Approach B (Hybrid):
 * - Material Kolor generates tonal palettes (backgrounds, surfaces, text colors)
 * - Override specific slots with exact brand colors (muted gold, emerald green)
 *
 * Brand colors (dark mode - source of truth):
 * - Charcoal #0A1828: ECB/Bank of England style background (seed color)
 * - Steel Blue #16213E: Military-grade secure system aesthetic
 * - Muted Gold #BFA574: Desaturated banking-house heraldry (kept exact)
 * - Emerald Green #0A8C6B: Government "secure/verified" standard (kept exact)
 *
 * Benefits:
 * - Material You's CAM16-based HCT color space for perceptual uniformity
 * - Automatic WCAG contrast compliance
 * - Cross-platform: Same colors generated for iOS & Android
 * - Cached for performance
 */
@Composable
fun CladTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Material Kolor generates tonal palette from charcoal seed color
    // Then we override with exact brand gold and emerald green
    val colorScheme = if (darkTheme) {
        ColorAdapter.getDarkColorScheme()
    } else {
        ColorAdapter.getLightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CladTypography,
        content = content
    )
}
