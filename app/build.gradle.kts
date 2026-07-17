import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Release signing is loaded from keystore.properties (gitignored) - the password is
// never inlined here. Absent that file, the build falls back to the debug key so it
// stays testable; a real release must be signed with your own keystore.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasReleaseKeystore) FileInputStream(keystorePropertiesFile).use { load(it) }
}

// Fail a RELEASE build outright if there's no keystore, instead of silently signing
// with the debug key (whose password is the public "android" and is identical on every
// machine - anyone could then ship an update over yours). Debug builds are unaffected.
if (!hasReleaseKeystore) {
    gradle.taskGraph.whenReady {
        val wantsRelease = allTasks.any { t ->
            val n = t.name
            n.endsWith("Release") && (n.startsWith("assemble") || n.startsWith("bundle") ||
                n.startsWith("package") || n.startsWith("install"))
        }
        if (wantsRelease) throw GradleException(
            "keystore.properties not found — refusing to sign a release with the public debug key. " +
            "Copy keystore.properties.example to keystore.properties and fill it in " +
            "(generate your keystore first). assembleDebug still works without it."
        )
    }
}

android {
    namespace = "dev.anpo.f1"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.anpo.f1"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        // No analytics, no crash reporting, no ad SDK. Nothing phones home.
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true          // R8: shrink + obfuscate
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Your release keystore ONLY - no debug fallback. If it's absent the build is
            // failed by the taskGraph guard above before it can produce an APK.
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val compose = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(compose)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room — feeds.db only. Disposable cache, excluded from backup.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
}
