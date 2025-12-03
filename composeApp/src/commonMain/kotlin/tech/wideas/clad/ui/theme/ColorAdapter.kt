package tech.wideas.clad.ui.theme

// Re-export ColorAdapter from shared module
// The shared module's ColorAdapter is in tech.wideas.clad.theme package
import tech.wideas.clad.theme.ColorAdapter as SharedColorAdapter

object ColorAdapter {
    fun getLightColorScheme() = SharedColorAdapter.getLightColorScheme()
    fun getDarkColorScheme() = SharedColorAdapter.getDarkColorScheme()
}
