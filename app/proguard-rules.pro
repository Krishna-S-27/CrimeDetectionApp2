# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard/index.html

# Keep Retrofit and OkHttp
-keep class retrofit2.** { *; }
-keep @retrofit2.http.** interface * { *; }
-keep class okhttp3.** { *; }

# Keep GSON models
-keep class com.krishna.crimedetection.models.** { *; }
