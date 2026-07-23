plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.primaloptima.scribe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.primaloptima.scribe"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing for a sideloadable APK — no keystore needed.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion("2.0.21")
            because("Align kotlin-stdlib with Kotlin compiler version 2.0.21")
        }
    }
}

dependencies {
    // Jetpack Compose BOM & core dependencies
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.airbnb.android:lottie-compose:6.4.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
    implementation("com.patrykandpatrick.vico:compose:2.0.0")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Core AndroidX
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Material Design 3
    implementation("com.google.android.material:material:1.12.0")

    // ViewModel + LiveData + coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // SAF document file helpers
    implementation("androidx.documentfile:documentfile:1.0.1")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.11.0")

    // Image loading (for sheet photos and cover images)
    implementation("io.coil-kt:coil:2.7.0")

    // Markwon Markdown Engine
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")

    // MPAndroidChart for statistics and word count analytics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Lottie for animations
    api("com.airbnb.android:lottie:6.4.1")

    // Color picker for theme editor (HSV wheel + sliders)
    implementation("com.github.skydoves:colorpickerview:2.2.4")

    // Palette — extract dominant colors from book cover images
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Modern Android 12+ splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Timber — smart debug logging (zero-cost in release builds)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
