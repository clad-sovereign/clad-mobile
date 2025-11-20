package tech.wideas.clad.security

import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation using LocalAuthentication (Face ID / Touch ID)
 * Simplified for PR #1
 */
class IOSBiometricAuth : BiometricAuth {

    override suspend fun isAvailable(): Boolean {
        // Simplified check for PR #1
        return true
    }

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String
    ): BiometricResult = suspendCoroutine { continuation ->
        val context = LAContext()
        val reason = buildString {
            append(title)
            if (subtitle.isNotEmpty()) {
                append("\n$subtitle")
            }
            if (description.isNotEmpty()) {
                append("\n$description")
            }
        }

        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason
        ) { success, error ->
            when {
                success -> {
                    continuation.resume(BiometricResult.Success)
                }
                error != null -> {
                    val errorCode = error.code
                    when (errorCode) {
                        -2L -> continuation.resume(BiometricResult.Cancelled) // User cancelled
                        -1L -> continuation.resume(BiometricResult.Cancelled) // System cancelled
                        -6L -> continuation.resume(BiometricResult.NotAvailable) // No biometrics enrolled
                        -7L -> continuation.resume(BiometricResult.NotAvailable) // Biometrics not available
                        else -> continuation.resume(BiometricResult.Error(error.localizedDescription))
                    }
                }
                else -> {
                    continuation.resume(BiometricResult.Error("Unknown authentication error"))
                }
            }
        }
    }
}

actual fun createBiometricAuth(): BiometricAuth = IOSBiometricAuth()
