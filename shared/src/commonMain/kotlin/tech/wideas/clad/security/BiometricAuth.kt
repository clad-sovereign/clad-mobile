package tech.wideas.clad.security

/**
 * Result of biometric authentication
 */
sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
    data object Cancelled : BiometricResult()
    data object NotAvailable : BiometricResult()
}

/**
 * Biometric authentication interface
 * Platform implementations:
 * - Android: BiometricPrompt
 * - iOS: LocalAuthentication (Face ID / Touch ID)
 */
interface BiometricAuth {
    /**
     * Check if biometric authentication is available on this device
     */
    suspend fun isAvailable(): Boolean

    /**
     * Authenticate user with biometric
     * @param title Title shown in the prompt
     * @param subtitle Subtitle shown in the prompt
     * @param description Description shown in the prompt
     * @return BiometricResult indicating success or failure
     */
    suspend fun authenticate(
        title: String,
        subtitle: String = "",
        description: String = ""
    ): BiometricResult
}

/**
 * Factory function to create platform-specific BiometricAuth
 */
expect fun createBiometricAuth(): BiometricAuth
