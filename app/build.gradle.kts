import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization")

    // ksp and hilt
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
    id("com.google.dagger.hilt.android") version "2.57.2"
}

android {
    namespace = "com.example.nanosonicproject"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nanosonicproject"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //hilt
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    //ksp
    implementation("com.google.dagger:dagger-compiler:2.57.2")
    ksp("com.google.dagger:dagger-compiler:2.57.2")
    //kotlinx
    // implementation(libs.kotlinx.coroutines.android)
    //icons
    implementation("androidx.compose.material:material-icons-extended")

    // ExoPlayer for audio playback (now using version catalog)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)

    // Coil for image loading (album art)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Media compat for notification MediaStyle
    implementation("androidx.media:media:1.7.1")

    //serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    //android auto (now using version catalog)
    implementation(libs.androidx.media3.session)
}