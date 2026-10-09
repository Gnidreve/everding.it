plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.everding.notepad"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.everding.notepad"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            // Wegwerfprojekt: debug-signiert, kein eigenes Keystore nötig.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // Für robustes Edge-to-Edge-/Insets-Handling (Android 15 erzwingt das ab targetSdk 35).
    implementation("androidx.core:core-ktx:1.13.1")
    // Sidebar: DrawerLayout liefert Edge-Swipe ohne eigenen Button.
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
}
