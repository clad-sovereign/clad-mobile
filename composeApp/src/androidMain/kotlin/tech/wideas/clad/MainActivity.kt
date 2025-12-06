package tech.wideas.clad

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import tech.wideas.clad.debug.DebugAccountSeeder
import tech.wideas.clad.debug.DebugConfigFactory
import tech.wideas.clad.di.AndroidActivityHolder
import tech.wideas.clad.di.commonModule
import tech.wideas.clad.di.platformModule
import tech.wideas.clad.di.viewModelModule

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Register this activity with the holder for biometric prompts
        AndroidActivityHolder.setActivity(this)

        // Set debug mode flag for cross-platform debug features
        DebugConfigFactory.setDebugMode(BuildConfig.DEBUG)

        // Configure Kermit logger for Android
        Logger.setLogWriters(platformLogWriter())
        // Set minimum log level based on build type
        Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Debug else Severity.Warn)

        setContent {
            KoinApplication(
                application = {
                    androidContext(this@MainActivity)
                    modules(platformModule, commonModule, viewModelModule)
                }
            ) {
                // Seed debug accounts on first launch (debug builds only)
                DebugAccountSeederEffect()
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the activity reference to prevent leaks
        AndroidActivityHolder.setActivity(null)
    }
}

/**
 * Effect composable that triggers debug account seeding on first launch.
 * Only runs in debug builds when database is empty.
 */
@Composable
private fun DebugAccountSeederEffect() {
    val seeder = koinInject<DebugAccountSeeder>()

    LaunchedEffect(Unit) {
        val result = seeder.seedIfNeeded()
        when (result) {
            is DebugAccountSeeder.SeedResult.Success -> {
                Logger.i("MainActivity") { "Debug accounts seeded: Alice=${result.aliceAccount != null}, Bob=${result.bobAccount != null}" }
            }
            is DebugAccountSeeder.SeedResult.Skipped -> {
                Logger.d("MainActivity") { "Debug seeding skipped: accounts already exist" }
            }
            is DebugAccountSeeder.SeedResult.NotDebugBuild -> {
                // Expected in release builds - no logging needed
            }
            is DebugAccountSeeder.SeedResult.BiometricCancelled -> {
                Logger.w("MainActivity") { "Debug seeding cancelled by user" }
            }
            is DebugAccountSeeder.SeedResult.Error -> {
                Logger.e("MainActivity") { "Debug seeding failed: ${result.message}" }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}