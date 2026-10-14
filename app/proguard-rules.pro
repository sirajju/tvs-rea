# Keep Moshi models
-keepclassmembers class com.sxplus.rider.data.remote.** { *; }
-keep class com.sxplus.rider.data.remote.** { *; }
# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn okhttp3.**
-dontwarn okio.**
