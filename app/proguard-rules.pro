# kotlinx.serialization keeps generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.anpo.f1.data.** { *** Companion; }
-keepclasseswithmembers class dev.anpo.f1.data.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.anpo.f1.data.**$$serializer { *; }
# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
