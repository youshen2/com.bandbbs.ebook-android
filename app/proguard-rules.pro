############################################
# 基础
############################################
-dontwarn **
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes KotlinMetadata

############################################
# Kotlin / 协程 / 反射相关
############################################
-keep class kotlinx.coroutines.** { *; }

############################################
# Kotlinx Serialization
############################################
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

############################################
# Room
############################################
-keep class androidx.room.** { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.**

############################################
# 你项目里的数据库实体 / DAO / 生成器
############################################
-keep class com.bandbbs.ebook.database.** { *; }

############################################
# 业务模型中带 @Serializable 的类
############################################
-keep class com.bandbbs.ebook.logic.Interconn$Message { *; }
-keep class com.bandbbs.ebook.logic.InterconnetFile$FileMessagesFromDevice** { *; }
-keep class com.bandbbs.ebook.logic.InterconnetFile$FileMessagesToSend** { *; }
-keep class com.bandbbs.ebook.logic.BookmarkData { *; }
-keep class com.bandbbs.ebook.logic.SyncReadingData { *; }

############################################
# 让日志类尽量在 release 中也可裁剪掉
############################################
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
