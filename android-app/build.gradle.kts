plugins {
    id("com.android.application")
}

val offlineKnowledgeSource = rootProject.projectDir.parentFile.resolve("android/assets/knowledge/hematology")
val offlineKnowledgeTarget = projectDir.resolve("src/main/assets/knowledge/hematology")

// Materialize the repository-level offline knowledge before Android source sets are evaluated.
copy {
    from(offlineKnowledgeSource)
    into(offlineKnowledgeTarget)
    include("**/*.json")
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

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}