# kotlinx.serialization: generierte Serializer behalten
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class de.einkaufsliste.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.einkaufsliste.app.**$$serializer { *; }
