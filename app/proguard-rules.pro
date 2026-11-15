# gomobile-generated bindings are called from native code.
-keep class go.** { *; }
-keep class uz.tulpor.tulpor.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class uz.tulpor.vpn.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
