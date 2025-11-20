package tech.wideas.clad.ui.connection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.BiometricResult

actual class BiometricAuthHandler(
    private val biometricAuth: BiometricAuth
) {
    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String
    ): BiometricResult {
        return biometricAuth.authenticate(title, subtitle, description)
    }
}

/**
 * Create BiometricAuthHandler with Android's FragmentActivity requirement
 */
@Composable
actual fun rememberBiometricAuthHandler(): BiometricAuthHandler {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
        ?: error("BiometricAuth requires FragmentActivity context")

    val biometricAuth = koinInject<BiometricAuth> { parametersOf(activity) }

    return remember(biometricAuth) {
        BiometricAuthHandler(biometricAuth)
    }
}
