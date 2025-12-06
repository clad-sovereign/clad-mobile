package tech.wideas.clad.debug

/**
 * Cross-platform debug configuration.
 *
 * This provides a way to check if the app is running in debug mode
 * across both Android and iOS platforms.
 *
 * Platform implementations:
 * - Android: Uses BuildConfig.DEBUG
 * - iOS: Uses #if DEBUG compiler flag (set via DebugConfigFactory)
 *
 * IMPORTANT: Debug-only features should NEVER be included in release builds.
 * Always guard debug code with `if (DebugConfig.isDebug)` checks.
 */
object DebugConfig {
    /**
     * Whether the app is running in debug mode.
     * Set by platform-specific initialization.
     *
     * - Android: Set automatically from BuildConfig.DEBUG
     * - iOS: Set from Swift using DebugConfigFactory.setDebugMode()
     *
     * Note: This is set once at app startup and never changes during runtime,
     * so thread-safety is not a concern.
     */
    var isDebug: Boolean = false
        internal set
}

/**
 * Factory for setting debug configuration from platform code.
 *
 * Android: Call from MainActivity or Application class
 * iOS: Call from Swift App init with #if DEBUG check
 */
object DebugConfigFactory {
    /**
     * Set whether the app is in debug mode.
     * Should be called early in app initialization.
     *
     * @param isDebug true for debug builds, false for release builds
     */
    fun setDebugMode(isDebug: Boolean) {
        DebugConfig.isDebug = isDebug
    }
}
