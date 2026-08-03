import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.safeargs)
}

val app_name = "Bluetooth LE Spam"
val releaseKey = file("release.jks")

android {
    namespace = "de.simon.dankelmann.bluetoothlespam"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.simon.dankelmann.bluetoothlespam"
        minSdk = 26
        targetSdk = compileSdk
        versionCode = 4
        versionName = "1.1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKey
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            resValue("string", "app_name", app_name)
            isMinifyEnabled = false
            signingConfig = if (releaseKey.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            resValue("string", "app_name", "$app_name Debug")
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }
}


dependencies {
    implementation(libs.airbnb.lottie)

    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.appcompat)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.legacy.support)
    implementation(libs.android.constraintlayout)
    implementation(libs.google.material)

    implementation(libs.room.runtime)

    // To use Kotlin Symbol Processing (KSP)
    ksp(libs.room.compiler)

    // optional - Kotlin Extensions and Coroutines support for Room
    //implementation(libs.room.ktx)

    // optional - RxJava2 support for Room
    //implementation(libs.room.rxjava2)

    // optional - RxJava3 support for Room
    implementation(libs.room.rxjava3)

    // optional - Guava support for Room, including Optional and ListenableFuture
    //implementation(libs.room.guava)

    // optional - Test helpers
    //testImplementation(libs.room.testing)

    // optional - Paging 3 Integration
    //implementation(libs.room.paging)
}
