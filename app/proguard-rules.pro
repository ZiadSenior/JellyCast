# ProGuard / R8 Rules for JellyCast
# Ensures all runtime reflection, serialization, network protocols, and third-party SDKs are preserved.

# ==============================================================================
# General Attributes & Debug Info
# ==============================================================================
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn javax.annotation.**

# ==============================================================================
# JellyCast Data Models & DTOs (Moshi JSON Serialization)
# ==============================================================================
# CRITICAL: Prevent Moshi reflection and field mapping from being stripped/obfuscated
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# ==============================================================================
# Moshi & Moshi Kotlin
# ==============================================================================
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# ==============================================================================
# Retrofit 2 & Jellyfin API Service
# ==============================================================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface retrofit2.Call
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface com.example.data.network.JellyfinApiService { *; }
-keepclassmembers interface com.example.data.network.JellyfinApiService { *; }

# ==============================================================================
# OkHttp 3 & Okio
# ==============================================================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }

# ==============================================================================
# Firebase & Google Play Services
# ==============================================================================
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ==============================================================================
# Coil Image Loader
# ==============================================================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ==============================================================================
# Kotlin Coroutines
# ==============================================================================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ==============================================================================
# AndroidX Lifecycle, ViewModel & Room
# ==============================================================================
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ==============================================================================
# DLNA / UPnP XML Pull Parser
# ==============================================================================
-keep class org.xmlpull.v1.** { *; }
-dontwarn org.xmlpull.v1.**
