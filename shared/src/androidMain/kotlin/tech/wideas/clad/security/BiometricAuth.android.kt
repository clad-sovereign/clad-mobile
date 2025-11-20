package tech.wideas.clad.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Android implementation using BiometricPrompt
 */
class AndroidBiometricAuth(private val context: Context) : BiometricAuth {

    override suspend fun isAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
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
        val activity = context as? FragmentActivity
            ?: return@suspendCoroutine continuation.resume(
                BiometricResult.Error("Context must be FragmentActivity")
            )

        val executor = ContextCompat.getMainExecutor(context)

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

// Singleton to hold the activity context
private var activityContext: FragmentActivity? = null

/**
 * Must be called from MainActivity.onCreate()
 */
fun initializeBiometricAuth(activity: FragmentActivity) {
    activityContext = activity
}

actual fun createBiometricAuth(): BiometricAuth {
    val context = activityContext
        ?: throw IllegalStateException("BiometricAuth not initialized. Call initializeBiometricAuth() first.")
    return AndroidBiometricAuth(context)
}
