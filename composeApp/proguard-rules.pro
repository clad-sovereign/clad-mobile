# Koin - Dependency Injection Framework
# Keep Koin modules and DSL classes to prevent reflection issues
-keep class tech.wideas.clad.di.** { *; }
-keep class * extends org.koin.core.module.Module { *; }

# Keep Koin DSL functions
-keepclassmembers class org.koin.core.** { *; }
-keepclassmembers class org.koin.android.** { *; }

# Keep all classes used for dependency injection
-keep class tech.wideas.clad.substrate.SubstrateClient { *; }
-keep class tech.wideas.clad.data.SettingsRepository { *; }
-keep class tech.wideas.clad.security.** { *; }

# Keep ViewModels
-keep class tech.wideas.clad.ui.** extends androidx.lifecycle.ViewModel { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Ktor - HTTP client
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-keep,includedescriptorclasses class tech.wideas.clad.**$$serializer { *; }
-keepclassmembers class tech.wideas.clad.** {
    *** Companion;
}
-keepclasseswithmembers class tech.wideas.clad.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

# Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }

# Remove logging in production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# General Android rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
