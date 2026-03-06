# NbViewer ProGuard / R8 rules

# ---- kotlinx.serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.nbviewer.data.model.**$$serializer { *; }
-keepclassmembers class com.nbviewer.data.model.** {
    *** Companion;
}
-keep class com.nbviewer.data.model.** { *; }

# ---- Domain models ----
-keep class com.nbviewer.domain.model.** { *; }

# ---- Markwon ----
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# ---- Kotlin / Android standard ----
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    <init>();
}
