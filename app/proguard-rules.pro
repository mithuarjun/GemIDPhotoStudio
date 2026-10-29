# Android Compose rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Data Models
-keep class com.gemid.photostudio.data.model.** { *; }

# Google ML Kit (Selfie Segmentation & Face Detection)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.tasks.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# CameraX & Guava
-dontwarn com.google.common.**
