package tech.wideas.clad.security

import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation using LocalAuthentication (Face ID / Touch ID)
 */
class IOSBiometricAuth : BiometricAuth {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null
        )
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
                    val nsError = error as NSError
                    val errorCode = nsError.code
                    when (errorCode) {
                        -2L -> continuation.resume(BiometricResult.Cancelled) // LAErrorUserCancel
                        -1L -> continuation.resume(BiometricResult.Cancelled) // LAErrorSystemCancel
                        -4L -> continuation.resume(BiometricResult.Cancelled) // LAErrorAppCancel
                        -6L -> continuation.resume(BiometricResult.NotAvailable) // LAErrorBiometryNotEnrolled
                        -7L -> continuation.resume(BiometricResult.NotAvailable) // LAErrorBiometryNotAvailable
                        -8L -> continuation.resume(BiometricResult.Error("Too many failed attempts. Please try again later.")) // LAErrorBiometryLockout
                        else -> continuation.resume(BiometricResult.Error(nsError.localizedDescription ?: "Authentication failed"))
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
