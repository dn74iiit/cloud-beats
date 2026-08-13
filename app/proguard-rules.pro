# CloudBeats ProGuard Rules

# Keep MSAL classes
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.aad.** { *; }

# Keep Microsoft Graph SDK
-keep class com.microsoft.graph.** { *; }
-keep class com.microsoft.kiota.** { *; }

# Keep Room entities
-keep class com.cloudbeats.app.data.local.entities.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Media3
-keep class androidx.media3.** { *; }

# Fix R8 missing classes
-dontwarn com.google.auto.value.AutoValue
-dontwarn reactor.blockhound.integration.**
