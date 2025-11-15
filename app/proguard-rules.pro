-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

-renamesourcefileattribute SourceFile

# ============================================
# Kotlin 相关
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ============================================
# Kotlinx Serialization
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bandbbs.ebook.**$$serializer { *; }
-keepclassmembers class com.bandbbs.ebook.** {
    *** Companion;
}
-keepclasseswithmembers class com.bandbbs.ebook.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================
# Room 数据库
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ============================================
# Jetpack Compose
# ============================================
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn androidx.compose.**

-dontwarn androidx.**

-keep class com.bandbbs.ebook.App { *; }
-keep class com.bandbbs.ebook.MainActivity { *; }
-keep @androidx.room.Entity class com.bandbbs.ebook.database.** { *; }
-keep class com.bandbbs.ebook.database.** { *; }
-keep interface com.bandbbs.ebook.database.**Dao { *; }
-keep,allowobfuscation class com.bandbbs.ebook.ui.model.** { *; }
-keep,allowobfuscation class com.bandbbs.ebook.logic.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification

-keep,allowobfuscation class coil.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# juniversalchardet
-keep,allowobfuscation class org.mozilla.universalchardet.** { *; }

# xms-wearable-lib
-keep class com.xiaomi.** { *; }
-dontwarn com.xiaomi.**

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-repackageclasses 'a'
-flattenpackagehierarchy 'a'

-mergeinterfacesaggressively
-overloadaggressively

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}