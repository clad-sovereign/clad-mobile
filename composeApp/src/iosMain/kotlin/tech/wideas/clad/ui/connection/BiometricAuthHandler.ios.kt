package tech.wideas.clad.ui.connection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.BiometricResult
import tech.wideas.clad.security.IOSBiometricAuth

actual class BiometricAuthHandler(
    private val biometricAuth: BiometricAuth = IOSBiometricAuth()
) {
    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String
    ): BiometricResult {
        return biometricAuth.authenticate(title, subtitle, description)
    }
}

@Composable
actual fun rememberBiometricAuthHandler(): BiometricAuthHandler {
    return remember {
        BiometricAuthHandler()
    }
}
