import SwiftUI
import Shared

/// iOS Color Adapter using Material You's Tonal Palette system (via SKIE)
///
/// Approach B (Hybrid):
/// - Kotlin ColorAdapter uses Material Kolor to generate tonal palettes
/// - iOS calls Kotlin function via SKIE to get the same Material You-generated colors
/// - Exact brand colors (muted gold, emerald green) are preserved across platforms
///
/// Benefits:
/// - 100% cross-platform color consistency (iOS uses exact same algorithm as Android)
/// - Perceptually uniform tonal variants via Material You's CAM16-based HCT color space
/// - WCAG contrast compliant
/// - Cached for performance
struct ColorAdapterIOS {

    /// Get adaptive color scheme from Kotlin ColorAdapter via SKIE
    /// This ensures iOS and Android use the exact same Material You-generated colors
    static func getColorScheme(for scheme: SwiftUI.ColorScheme) -> CladColors.ColorScheme {
        let isDark = (scheme == .dark)

        // Call Kotlin function via SKIE to get Material Kolor-generated colors
        // Kotlin's ColorAdapter object is exposed as ColorAdapter.shared via SKIE
        let kotlinPalette = ColorAdapter.shared.getColorPaletteForIOS(isDark: isDark)

        // Convert Kotlin's Long color values to SwiftUI Colors
        return CladColors.ColorScheme(
            background: Color(argb: kotlinPalette.background),
            surface: Color(argb: kotlinPalette.surface),
            surfaceVariant: Color(argb: kotlinPalette.surfaceVariant),
            primary: Color(argb: kotlinPalette.primary),
            primaryContainer: Color(argb: kotlinPalette.primary).opacity(0.3),  // Lighter variant
            onPrimary: Color(argb: kotlinPalette.onPrimary),
            secondary: Color(argb: kotlinPalette.secondary),
            onSecondary: Color(argb: kotlinPalette.onSecondary),
            onBackground: Color(argb: kotlinPalette.onBackground),
            onSurface: Color(argb: kotlinPalette.onSurface),
            error: Color(argb: kotlinPalette.error),
            tertiary: Color(argb: kotlinPalette.tertiary),
            onSurfaceVariant: Color(argb: kotlinPalette.onSurfaceVariant),
            messageSent: Color(argb: kotlinPalette.messageSent),
            messageReceived: Color(argb: kotlinPalette.messageReceived)
        )
    }
}

/// Extension to create SwiftUI Color from Compose's ARGB Long value
extension Color {
    /// Create Color from Compose UI's color value (ARGB format stored as Long)
    /// Compose Color.value is ULong where ARGB is stored in UPPER 32 bits
    /// Format: 0xAARRGGBB00000000
    init(argb: Int64) {
        let uValue = UInt64(bitPattern: argb)

        // Extract ARGB components from upper 32 bits (shift right by 32 first)
        let colorValue = uValue >> 32
        let alpha = Double((colorValue >> 24) & 0xFF) / 255.0
        let red = Double((colorValue >> 16) & 0xFF) / 255.0
        let green = Double((colorValue >> 8) & 0xFF) / 255.0
        let blue = Double(colorValue & 0xFF) / 255.0

        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}
