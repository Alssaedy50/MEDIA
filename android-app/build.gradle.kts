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
        versionCode = 2
        versionName = "0.2.0"
    }

    buildFeatures { buildConfig = true }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    sourceSets.getByName("main").assets.srcDir(offlineAssetsDir)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}