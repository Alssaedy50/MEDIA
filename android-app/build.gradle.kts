plugins {
    id("com.android.application")
}

val mediaWebUrl = providers.gradleProperty("MEDIA_WEB_URL")
    .orElse("http://10.0.2.2:8000/")
    .get()
    .trimEnd('/') + "/"

android {
    namespace = "com.media.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.media.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "MEDIA_WEB_URL", "\"$mediaWebUrl\"")
    }

    buildFeatures { buildConfig = true }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
