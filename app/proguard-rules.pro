# kotlinx.serialization: keep generated serializers for @Serializable nav routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.slowdown.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.slowdown.app.**$$serializer { *; }
# Manifest components are kept by AGP; keep VPN service and receivers explicitly.
-keep class com.slowdown.throttle.SlowDownVpnService { *; }
-keep class * extends android.content.BroadcastReceiver
