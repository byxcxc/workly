# Workly ProGuard / R8 rules.
#
# Room generates implementations that are referenced reflectively from the
# generated database class, so keep them.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# kotlinx.serialization keeps its generated serializers on the companion object.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.workly.app.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.workly.app.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin coroutines internals.
-dontwarn kotlinx.coroutines.**
