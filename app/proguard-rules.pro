# kotlinx.serialization keeps generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.pokhriyal_anmol.f1.data.** { *** Companion; }
-keepclasseswithmembers class io.github.pokhriyal_anmol.f1.data.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class io.github.pokhriyal_anmol.f1.data.**$$serializer { *; }
# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
