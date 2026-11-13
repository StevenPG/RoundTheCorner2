# Project-specific R8 rules. Most libraries we use (Hilt, Compose, DataStore,
# kotlinx.serialization) ship their own consumer rules, so this stays small.

# Navigation 3 keys are @Serializable and saved across process death.
-keepclassmembers @kotlinx.serialization.Serializable class com.stevenpg.roundthecorner2.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
