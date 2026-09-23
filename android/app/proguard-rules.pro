# Proguard rules for Cinematic Photo Animator
-keep class com.cinematic.photoanimator.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
