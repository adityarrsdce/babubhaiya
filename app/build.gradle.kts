plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("com.google.gms.google-services") //  Firebase GMS plugin
}

android {
    namespace = "com.babu.appp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.babu.appp"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.2.7"

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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    implementation("androidx.compose.ui:ui")
    implementation("com.google.accompanist:accompanist-webview:0.34.0")

    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.play.services.measurement.api)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")


    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.compose.foundation:foundation:1.6.1")

    // build.gradle (Module)
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.gms:play-services-ads:23.0.0")

    implementation("androidx.preference:preference-ktx:1.2.1")



    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // 🔄 Play Store In-App Update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    implementation("com.airbnb.android:lottie-compose:6.4.0")





    // ✅ FIXED VERSION
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
}