# Keep NanoHTTPD server classes
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# Google Mobile Ads / Play Services Keep rules
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep class com.google.android.gms.ads.identifier.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep models & config data classes
-keepclassmembers class com.localdrop.ads.AdConfig { *; }
-keepclassmembers class com.localdrop.model.** { *; }

# Strip Android Log calls in release to save CPU cycles
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
