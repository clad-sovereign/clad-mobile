package tech.wideas.clad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Institutional colors: Dark blue + Gold
private val CladDarkBlue = Color(0xFF1A2332)
private val CladNavyBlue = Color(0xFF2C3E50)
private val CladGold = Color(0xFFD4AF37)
private val CladLightGold = Color(0xFFE5C158)
private val CladWhite = Color(0xFFF8F9FA)
private val CladGray = Color(0xFFB0B8C1)
private val CladLightGray = Color(0xFFE8EBF0)
private val CladErrorRed = Color(0xFFDC3545)

private val DarkColorScheme = darkColorScheme(
    primary = CladGold,
    onPrimary = CladDarkBlue,
    secondary = CladNavyBlue,
    onSecondary = CladWhite,
    background = CladDarkBlue,
    onBackground = CladWhite,
    surface = CladNavyBlue,
    onSurface = CladWhite,
    surfaceVariant = Color(0xFF34495E),
    onSurfaceVariant = CladLightGray,
    error = CladErrorRed,
    onError = CladWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CladNavyBlue,
    onPrimary = CladWhite,
    secondary = CladGold,
    onSecondary = CladDarkBlue,
    background = CladWhite,
    onBackground = CladDarkBlue,
    surface = CladLightGray,
    onSurface = CladDarkBlue,
    surfaceVariant = Color(0xFFF0F3F7),
    onSurfaceVariant = CladNavyBlue,
    error = CladErrorRed,
    onError = CladWhite
)

@Composable
fun CladTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CladTypography,
        content = content
    )
}
