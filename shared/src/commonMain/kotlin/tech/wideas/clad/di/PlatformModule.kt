package tech.wideas.clad.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin module
 * Each platform provides its own implementation
 */
expect val platformModule: Module
