package tech.wideas.clad

import androidx.compose.runtime.*
import tech.wideas.clad.ui.AppNavigation
import tech.wideas.clad.ui.theme.CladTheme

@Composable
fun App() {
    CladTheme {
        AppNavigation()
    }
}
