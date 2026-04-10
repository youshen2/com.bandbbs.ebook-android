# ============================================
# 基础性能与体积优化
# ============================================
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# 恢复默认优化，仅禁用已知在某些架构上有问题的算术优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast

-dontusemixedcaseclassnames
-dontpreverify
-verbose

# ============================================
# 属性保留
# ============================================
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ============================================
# 日志剔除
# ============================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
}

# ============================================
# Kotlin & Coroutines
# ============================================
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

-dontwarn kotlin.Metadata
-keepattributes !KotlinMetadata

# 只有在需要反射获取 Kotlin 类信息时才保留 Metadata
# -keep class kotlin.Metadata { *; }

-keepclassmembers class **$WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ============================================
# Kotlinx Serialization
# ============================================
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keep,includedescriptorclasses class com.bandbbs.ebook.**$$serializer { *; }
-keepclassmembers class com.bandbbs.ebook.** {
    *** Companion;
}
-keepclasseswithmembers class com.bandbbs.ebook.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.bandbbs.ebook.** { *; }

# ============================================
# Room 数据库
# ============================================
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *
-keep @androidx.room.Entity class com.bandbbs.ebook.database.**

# ============================================
# 第三方库处理
# ============================================
-keep class com.xiaomi.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }

-dontwarn androidx.**
-dontwarn okhttp3.**
-dontwarn okio.**

-dontwarn java.awt.**
-dontwarn javax.security.**
-dontwarn com.sun.javadoc.**
-dontwarn sun.misc.Unsafe
-dontwarn org.apache.poi.**

# 保留 Native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class javax.xml.stream.** { *; }

-dontwarn com.tom_roush.pdfbox.pdmodel.font.FileSystemFontProvider
-dontwarn javax.xml.stream.**

-keep class com.google.re2j.** { *; }

-keep class com.gemalto.jp2.** { *; }
-dontwarn com.gemalto.jp2.**