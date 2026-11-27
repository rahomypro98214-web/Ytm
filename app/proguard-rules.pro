# Preserve line numbers for readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations used by Firestore, Room, and Compose
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep @Keep annotated classes and members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Preserve Firestore data models used via reflection
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }

# Preserve Room entities and generated DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep Kotlin Serialization / Coroutines internals clean
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
