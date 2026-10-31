plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.stikr.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stikr.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.4"
    }

    // Repo keystore (PKCS12): every release APK gets the same signature, so
    // updates install over the old version instead of conflicting. Anyone with
    // repo access can sign as this app — accepted tradeoff for a sideloaded
    // app; back up keystore/release.p12 regardless.
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/release.p12")
            storeType = "PKCS12"
            storePassword = "stikr-release"
            keyAlias = "stikr"
            keyPassword = "stikr-release"
        }
    }

    buildTypes {
        release {
            // Persistent repo keystore so every release carries the same
            // signature and installs cleanly over the previous version.
            signingConfig = signingConfigs.getByName("release")
            // Minify stays off until a release has been exercised on a device:
            // the cut-out path goes through Play services (ML Kit) and a
            // mis-kept class would only show up at runtime.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Robolectric's FileDescriptor handling reflects into java.io on JDK 17.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    // Install bundled ART profiles for sideloaded APKs, after the first frame.
    // Version 1.4 supports Android 15+ without per-platform library updates.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Auto cut-out: on-device subject segmentation, model downloaded by Play services on first use.
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")

    // Screen render tests: whole screens captured to build/ui-renders/ on the JVM.
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
}
