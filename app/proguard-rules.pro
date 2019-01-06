-dontobfuscate
-optimizations !code/allocation/variable
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-keep class com.gracenote.gnsdk.** {
  *;
}
-keep class androidx.appcompat.app.**{
  *;
}

-keepclassmembers class ** {
    public void onEvent*(**);
}


