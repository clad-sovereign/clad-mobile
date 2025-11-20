package tech.wideas.clad.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Cross-platform color adapter using Material You's Tonal Palette system
 *
 * Uses Material Kolor (KMP library) to generate perceptually uniform color schemes
 * from dark brand colors, then overrides with exact institutional brand colors.
 *
 * Approach B (Hybrid):
 * - Material Kolor generates tonal palettes (backgrounds, surfaces, text colors)
 * - Override specific slots with exact brand colors (gold, emerald)
 * - Shared between iOS (via SKIE) and Android
 *
 * Brand Colors (Dark Mode - Source of Truth):
 * - Charcoal #0A1828: ECB/Bank of England style background
 * - Steel Blue #16213E: Military-grade secure system aesthetic
 * - Muted Gold #BFA574: Desaturated banking-house heraldry
 * - Emerald Green #0A8C6B: Government "secure/verified" standard
 */
object ColorAdapter {

    // MARK: - Brand Colors (Source of Truth)

    object BrandColors {
        val charcoal = Color(0xFF0A1828)        // Very dark navy/charcoal (ECB, BoE style)
        val steelBlue = Color(0xFF16213E)       // Deep steel blue (military-grade secure)
        val mutedGold = Color(0xFFBFA574)       // Muted institutional gold (banking heraldry)
        val emeraldGreen = Color(0xFF0A8C6B)    // Subtle emerald green (government "secure/verified")
        val offWhite = Color(0xFFE5E5E5)        // Off-white/light grey
        val coolGray = Color(0xFFA0AEC0)        // Cool grey (IMF, World Bank dashboards)
        val lightGray = Color(0xFFE8EBF0)       // Light surface variants
        val errorRed = Color(0xFFDC3545)        // Error states
    }

    // MARK: - Platform Text Colors (iOS HIG / Material Design compliance)

    private val textLabelLight = Color(0xFF1C1C1E)      // Near-black for light mode primary text
    private val textLabelDark = Color(0xFFF2F2F7)       // Light gray for dark mode primary text
    private val textSecondaryLight = Color(0xFF3C3C43)  // Secondary text light mode
    private val textSecondaryDark = Color(0xFFAEAEB2)   // Secondary text dark mode

    // MARK: - Cached Color Schemes

    private var cachedLightScheme: ColorScheme? = null
    private var cachedDarkScheme: ColorScheme? = null

    /**
     * Get light color scheme (cached for performance)
     * Material Kolor generates base palette, then we override with brand colors
     */
    fun getLightColorScheme(): ColorScheme {
        cachedLightScheme?.let { return it }

        // Step 1: Generate base palette from charcoal seed using Material You algorithm
        val basePalette = dynamicColorScheme(
            seedColor = BrandColors.charcoal,
            isDark = false,
            style = PaletteStyle.TonalSpot,  // Subtle, institutional style
            contrastLevel = 0.0  // Standard contrast
        )

        // Step 2: Override with exact brand colors (Approach B - Hybrid)
        val computed = basePalette.copy(
            // Keep your exact brand gold as primary accent
            primary = BrandColors.mutedGold,
            onPrimary = BrandColors.charcoal,

            // Keep your exact emerald green for "secure/verified" states
            tertiary = BrandColors.emeraldGreen,
            onTertiary = Color(0xFFFFFFFF),

            // Use Material Kolor's calculated backgrounds (perfect light mode tones)
            background = basePalette.background,
            onBackground = textLabelLight,  // iOS/Material standard near-black

            // Override surface with light gray for better card contrast in light mode
            surface = BrandColors.lightGray,  // #E8EBF0 - subtle blue-tinted gray
            onSurface = textLabelLight,

            // Surface variants for cards/elevated components
            surfaceVariant = basePalette.surfaceVariant,
            onSurfaceVariant = textSecondaryLight,

            // Keep brand error color
            error = BrandColors.errorRed,
            onError = Color(0xFFFFFFFF)
        )

        cachedLightScheme = computed
        return computed
    }

    /**
     * Get dark color scheme (cached for performance)
     * Uses brand colors directly as they're designed for dark mode
     */
    fun getDarkColorScheme(): ColorScheme {
        cachedDarkScheme?.let { return it }

        // Step 1: Generate base palette from charcoal seed
        val basePalette = dynamicColorScheme(
            seedColor = BrandColors.charcoal,
            isDark = true,
            style = PaletteStyle.TonalSpot,
            contrastLevel = 0.0
        )

        // Step 2: Override with exact brand colors
        val computed = basePalette.copy(
            // Brand colors are designed for dark mode, use directly
            primary = BrandColors.mutedGold,
            onPrimary = BrandColors.charcoal,

            secondary = BrandColors.steelBlue,
            onSecondary = BrandColors.offWhite,

            tertiary = BrandColors.emeraldGreen,
            onTertiary = BrandColors.offWhite,

            // Dark backgrounds
            background = BrandColors.charcoal,
            onBackground = textLabelDark,  // iOS/Material standard light gray

            // Steel blue surfaces
            surface = BrandColors.steelBlue,
            onSurface = textLabelDark,

            surfaceVariant = Color(0xFF1E3A5F),  // Slightly lighter steel blue
            onSurfaceVariant = BrandColors.coolGray,

            error = BrandColors.errorRed,
            onError = BrandColors.offWhite
        )

        cachedDarkScheme = computed
        return computed
    }

    /**
     * For iOS: Convert ColorScheme to simple data structure
     * iOS will call this via SKIE to get the Material Kolor-generated colors
     */
    data class ColorPalette(
        val background: Long,
        val surface: Long,
        val primary: Long,
        val onPrimary: Long,
        val secondary: Long,
        val onSecondary: Long,
        val tertiary: Long,
        val onTertiary: Long,
        val onBackground: Long,
        val onSurface: Long,
        val error: Long,
        val onError: Long,
        val surfaceVariant: Long,
        val onSurfaceVariant: Long,
        val messageSent: Long,
        val messageReceived: Long,
        val streamBackground: Long
    )

    /**
     * Export color scheme as simple data structure for iOS
     * This function is called from Swift via SKIE
     */
    fun getColorPaletteForIOS(isDark: Boolean): ColorPalette {
        val scheme = if (isDark) getDarkColorScheme() else getLightColorScheme()

        return ColorPalette(
            background = scheme.background.value.toLong(),
            surface = scheme.surface.value.toLong(),
            primary = scheme.primary.value.toLong(),
            onPrimary = scheme.onPrimary.value.toLong(),
            secondary = scheme.secondary.value.toLong(),
            onSecondary = scheme.onSecondary.value.toLong(),
            tertiary = scheme.tertiary.value.toLong(),
            onTertiary = scheme.onTertiary.value.toLong(),
            onBackground = scheme.onBackground.value.toLong(),
            onSurface = scheme.onSurface.value.toLong(),
            error = scheme.error.value.toLong(),
            onError = scheme.onError.value.toLong(),
            surfaceVariant = scheme.surfaceVariant.value.toLong(),
            onSurfaceVariant = scheme.onSurfaceVariant.value.toLong(),
            // Additional colors for iOS
            messageSent = Color(0xFF5B8DBE).value.toLong(),  // Muted institutional blue
            messageReceived = BrandColors.emeraldGreen.value.toLong(),
            streamBackground = if (isDark) BrandColors.charcoal.value.toLong() else Color(0xFFF5F7FA).value.toLong()
        )
    }
}
