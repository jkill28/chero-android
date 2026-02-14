# Credential Manager
-keep class androidx.credentials.** { *; }
-keep interface androidx.credentials.** { *; }

# Google ID
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep interface com.google.android.libraries.identity.googleid.** { *; }

# WebAuth Interface (JS Bridge)
-keepclassmembers class com.cher.app.MainActivity$WebAuthInterface {
    @android.webkit.JavascriptInterface <methods>;
}
