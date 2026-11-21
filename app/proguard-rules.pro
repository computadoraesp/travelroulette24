# Keep kotlinx.serialization generated serializers and models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.travelroulette24.**$$serializer { *; }
-keepclassmembers class com.example.travelroulette24.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.travelroulette24.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Retrofit / OkHttp interfaces & annotations
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
