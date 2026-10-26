# Keep line numbers so Play Console stack traces stay readable once deobfuscated.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson fills RemoteApod by reflection using its field names. Gson 2.8.2 ships no R8 rules of its own.
-keepattributes Signature,*Annotation*
-keep class uk.co.jakelee.apodwallpaper.config.RemoteApod { *; }

# Optional OkHttp 3.10 dependencies, used only when present at runtime.
-dontwarn javax.annotation.Nullable
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.OpenSSLProvider

# WorkManager's Room 2.6.1 keeps RoomDatabase subclasses but not their constructors, which R8
# full mode strips, so WorkManager crashes on startup. Newer Room versions ship this rule themselves.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
