# Wiva Android — R8 (Hilt, OkHttp, kotlinx.serialization, Room)

# ── Hilt ──────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class **_HiltComponents* { *; }
-keep class **_Hilt_* { *; }

-optimizations !class/merging/vertical,!class/merging/horizontal

# ── javax.inject / Dagger ────────────────────────────────────────────────────
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.wiva.android.**$$serializer { *; }
-keepclassmembers class com.wiva.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.wiva.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ── OkHttp / Retrofit ─────────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep interface com.wiva.android.data.remote.** { *; }
-keepclassmembers interface com.wiva.android.data.remote.** { *; }
-keep class com.wiva.android.data.remote.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── Media3 (промо-видео wiva_electron) ─────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── usb-serial-for-android (контроллер USB UART) ───────────────────────────────
-keep class com.hoho.android.usbserial.** { *; }

# ── android-serialport-api (нативный UART /dev/ttyS*) ──────────────────────────
-keep class android_serialport_api.** { *; }
-dontwarn android_serialport_api.**

# ── Java-WebSocket (телеметрия WS, лог RFC6455 ping/pong) ────────────────────
-keep class org.java_websocket.** { *; }
-dontwarn org.slf4j.**

# ── Timber ────────────────────────────────────────────────────────────────────
-keep class timber.log.** { *; }

# ── General ───────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

# API 25: Compose SystemPropertiesCompat / R8
-keep class android.os.SystemProperties { *; }
-keepclassmembers class android.os.SystemProperties { *; }
