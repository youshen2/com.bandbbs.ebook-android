# ============================================
# 基础性能与体积优化
# ============================================
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses 'a'
-flattenpackagehierarchy 'a'

# 恢复默认优化，仅禁用已知在某些架构上有问题的算术优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast

# 移除没必要的冗余扫描，提升编译速度
-dontusemixedcaseclassnames
-dontpreverify
-verbose

# ============================================
# 属性保留 (仅保留必要项)
# ============================================
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ============================================
# 强力日志剔除 (提升性能 & 减小体积)
# ============================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
    # 注意：保留 e(...) 和 w(...) 用于线上排查错误，若追求极致可一并剔除
}

# ============================================
# Kotlin & Coroutines
# ============================================
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
# 只有在需要反射获取 Kotlin 类信息时才保留 Metadata，否则建议注释掉以节省空间
# -keep class kotlin.Metadata { *; }

-keepclassmembers class **$WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ============================================
# Kotlinx Serialization (针对性保留而非包保留)
# ============================================
-keep,includedescriptorclasses class com.bandbbs.ebook.**$$serializer { *; }
-keepclassmembers class com.bandbbs.ebook.** {
    *** Companion;
}
-keepclasseswithmembers class com.bandbbs.ebook.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================
# Room 数据库 (优化：移除整个包的 keep)
# ============================================
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *
-keep @androidx.room.Entity class com.bandbbs.ebook.database.**

# ============================================
# 第三方库处理
# ============================================
# PDFBox & Xiaomi 库通常包含反射或 JNI，建议保守处理
-keep class com.xiaomi.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }

-dontwarn androidx.**
-dontwarn okhttp3.**
-dontwarn okio.**

# 保留 Native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# Enum 优化（R8 可以自动处理，但在某些复杂场景下手动指定可保平安）
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}