buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.android.gradlePlugin)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.kotlin.serialization.gradlePlugin)
        classpath(libs.kotlin.compose.gradlePlugin)
        classpath(libs.hilt.android.gradlePlugin)
        classpath(libs.ksp.gradlePlugin)
    }
}
