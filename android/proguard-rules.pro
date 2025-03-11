-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.ar.core.** { *; }
-keep class com.google.ar.sceneform.** { *; }

-keep class com.google.android.gms.location.FusedLocationProviderClient { *; }

-keep class com.google.ar.core.Config { *; }
-keep class com.google.ar.core.Session { *; }

-keepnames class com.google.ar.** { *; }
-dontwarn com.google.ar.**
