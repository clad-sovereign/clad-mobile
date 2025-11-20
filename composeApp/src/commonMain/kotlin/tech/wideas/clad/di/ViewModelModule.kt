package tech.wideas.clad.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import tech.wideas.clad.ui.connection.ConnectionViewModel

/**
 * Koin module for ViewModels
 */
val viewModelModule = module {
    viewModelOf(::ConnectionViewModel)
}
