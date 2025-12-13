// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

    // ksp
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

