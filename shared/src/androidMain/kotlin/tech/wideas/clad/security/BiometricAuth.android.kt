package tech.wideas.clad.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Android implementation using BiometricPrompt
 *
 * Note: Requires FragmentActivity for BiometricPrompt integration.
 * Should be injected via Koin factory with activity parameter.
 */
class AndroidBiometricAuth(private val activity: FragmentActivity) : BiometricAuth {

    override suspend fun isAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String
    ): BiometricResult = suspendCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            continuation.resume(BiometricResult.Cancelled)
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            continuation.resume(BiometricResult.NotAvailable)
                        }
                        else -> {
                            continuation.resume(BiometricResult.Error(errString.toString()))
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    // Don't resume here - user can retry
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

actual fun createBiometricAuth(): BiometricAuth {
    throw UnsupportedOperationException(
        "BiometricAuth is now managed by Koin dependency injection. " +
        "Use koinInject<BiometricAuth>() or constructor injection instead."
    )
}
