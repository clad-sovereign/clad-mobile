import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.skie)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Link SQLite3 for SQLDelight native driver
            linkerOpts("-lsqlite3")
        }

        // Configure cinterop for NovaCrypto (iOS crypto library)
        iosTarget.compilations.getByName("main") {
            cinterops {
                val NovaCrypto by creating {
                    defFile(project.file("src/nativeInterop/cinterop/NovaCrypto.def"))
                    includeDirs(project.file("src/nativeInterop/cinterop/headers"))
                }
            }
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            api(libs.koin.core)
            implementation(libs.kermit)
            // Compose dependencies needed for Material Kolor
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.material3)
            // Material Kolor for cross-platform color generation
            implementation(libs.material.kolor)
            // SQLDelight for local database
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment.ktx)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.google.tink)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            // Nova Substrate SDK for crypto operations (sr25519, ed25519, BIP39, SS58)
            implementation(libs.nova.substrate.sdk.core)
            // SQLDelight Android driver
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // SQLDelight Native driver for iOS
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.rules)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "tech.wideas.clad.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

// CI Test Configuration - Phase 1: Unit Tests Only
tasks.withType<Test> {
    // Exclude integration tests that require a real Substrate node
    exclude("**/SubstrateClient*IntegrationTest*")
    exclude("**/SubstrateClient*RpcTest*")
    exclude("**/SubstrateClient*ConcurrencyTest*")
    exclude("**/SubstrateClient*ReconnectionTest*")

    // These tests run in CI (no node required):
    // - BiometricAuthTest
    // - SecureStorageTest
    // - SettingsRepositoryTest
    // - ConnectionViewModelTest
}

sqldelight {
    databases {
        create("CladDatabase") {
            packageName.set("tech.wideas.clad.database")
        }
    }
}
