# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# WorkManager & Room Database rules (Fixes InitializationProvider / WorkDatabase crash)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep Data Models for Gson reflection (Fixes network serialization in release mode)
-keep class com.nandan.calorimeterapp.data.model.** { *; }
-keepclassmembers class com.nandan.calorimeterapp.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Google Mobile Ads SDK (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }

# Keep line numbers & attributes for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile