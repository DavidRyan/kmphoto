plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()

    jvmToolchain(17)

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":core-domain"))
            }
        }
    }
}

android {
    namespace = "com.example.kmpicture.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}
