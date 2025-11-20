package tech.wideas.clad

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import tech.wideas.clad.di.commonModule
import tech.wideas.clad.di.platformModule
import tech.wideas.clad.di.viewModelModule

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
                App()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}