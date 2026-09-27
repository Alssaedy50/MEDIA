plugins {
    id("com.android.application")
}

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

    // Offline Alpha knowledge is stored in the repository-level android/assets tree.
    // Expose that directory to Android AssetManager at runtime.
    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.projectDir.parentFile.resolve("android/assets"))
        }
    }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}