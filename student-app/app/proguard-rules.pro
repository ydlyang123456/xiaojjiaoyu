# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keep class com.studycheck.student.data.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
