// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Declared at root so the app module can apply it via alias. Generates
    // @Serializable companion serializers used by UserProgressSerializer.
    alias(libs.plugins.kotlin.serialization) apply false
}
