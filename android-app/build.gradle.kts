plugins {
    id("com.android.application")
}

// The offline Hematology knowledge lives at the repository level under android/assets.
// Register it as an assets source directory so it is packaged without duplicating the files.
val offlineAssetsDir = rootProject.projectDir.resolve("assets")

android {
    namespace = "com.media.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.media.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    sourceSets.getByName("main").assets.srcDir(offlineAssetsDir)
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment:2.8.5")
    implementation("androidx.navigation:navigation-ui:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for pure-JVM tests that exercise the actual offline retrieval code path.
    testImplementation("org.json:json:20240303")
}