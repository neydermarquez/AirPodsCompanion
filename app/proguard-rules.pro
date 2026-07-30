# Preserve signatures and annotations used by Android and Room.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

# Room generates the implementation by name and uses generated entity adapters.
-keep class com.soren.airpodscompanion.data.local.AirPodsDatabase_Impl { *; }
-keepclassmembers class com.soren.airpodscompanion.data.local.* {
    <init>(...);
}

# Android entry points instantiated from the manifest.
-keep class com.soren.airpodscompanion.MainActivity { *; }
-keep class com.soren.airpodscompanion.AirPodsWidgetConfigActivity { *; }
-keep class com.soren.airpodscompanion.AirPodsMonitorService { *; }
-keep class com.soren.airpodscompanion.AirPodsCompanionPresenceService { *; }
-keep class com.soren.airpodscompanion.AirPodsWidget { *; }
-keep class com.soren.airpodscompanion.BootReceiver { *; }
-keep class com.soren.airpodscompanion.NotificationActionReceiver { *; }
