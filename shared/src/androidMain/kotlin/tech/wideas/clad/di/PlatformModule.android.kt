package tech.wideas.clad.di

import androidx.fragment.app.FragmentActivity
import app.cash.sqldelight.db.SqlDriver
import org.koin.dsl.module
import tech.wideas.clad.crypto.MnemonicProvider
import tech.wideas.clad.crypto.createMnemonicProvider
import tech.wideas.clad.database.DriverFactory
import tech.wideas.clad.security.AndroidBiometricAuth
import tech.wideas.clad.security.AndroidKeyStorage
import tech.wideas.clad.security.AndroidSecureStorage
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.security.SecureStorage

/**
 * Singleton holder for the current activity reference.
 * Must be initialized by MainActivity before KeyStorage is used.
 */
object AndroidActivityHolder {
    private var currentActivity: FragmentActivity? = null

    fun setActivity(activity: FragmentActivity?) {
        currentActivity = activity
    }

    fun getActivity(): FragmentActivity {
        return currentActivity ?: throw IllegalStateException(
            "No activity available. Make sure MainActivity.onCreate has been called."
        )
    }

    fun getActivityProvider(): () -> FragmentActivity = { getActivity() }
}

/**
 * Android-specific Koin module
 */
actual val platformModule = module {
    // SecureStorage using application context
    single<SecureStorage> {
        AndroidSecureStorage(get())
    }

    // BiometricAuth factory - requires FragmentActivity parameter
    // Usage: val biometricAuth = koinInject<BiometricAuth>(parameters = { parametersOf(activity) })
    factory<BiometricAuth> { params ->
        val activity = params.get<FragmentActivity>()
        AndroidBiometricAuth(activity)
    }

    // KeyStorage singleton - uses AndroidActivityHolder for activity reference
    single<KeyStorage> {
        AndroidKeyStorage(get(), AndroidActivityHolder.getActivityProvider())
    }

    // MnemonicProvider for BIP39 operations
    single<MnemonicProvider> {
        createMnemonicProvider()
    }

    // SQLDelight driver for Android
    single<SqlDriver> {
        DriverFactory(get()).createDriver()
    }
}
