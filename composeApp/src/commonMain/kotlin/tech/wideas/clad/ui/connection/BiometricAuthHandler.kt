package tech.wideas.clad.ui.connection

import androidx.compose.runtime.Composable
import tech.wideas.clad.security.BiometricResult

/**
 * Platform-specific handler for triggering biometric authentication
 * This is needed because Android requires FragmentActivity context
 */
expect class BiometricAuthHandler {
    suspend fun authenticate(
        title: String,
        subtitle: String = "",
        description: String = ""
    ): BiometricResult
}

/**
 * Platform-specific composable to create BiometricAuthHandler
 */
@Composable
expect fun rememberBiometricAuthHandler(): BiometricAuthHandler
