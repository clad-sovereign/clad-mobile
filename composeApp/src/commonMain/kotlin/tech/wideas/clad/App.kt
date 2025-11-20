package tech.wideas.clad

import androidx.compose.runtime.*
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.security.createSecureStorage
import tech.wideas.clad.substrate.SubstrateClient
import tech.wideas.clad.ui.AppNavigation
import tech.wideas.clad.ui.theme.CladTheme

@Composable
fun App() {
    // Initialize dependencies
    val secureStorage = remember { createSecureStorage() }
    val settingsRepository = remember { SettingsRepository(secureStorage) }
    val substrateClient = remember { SubstrateClient() }

    CladTheme {
        AppNavigation(
            substrateClient = substrateClient,
            settingsRepository = settingsRepository
        )
    }
}
