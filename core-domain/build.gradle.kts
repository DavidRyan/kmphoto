plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)
}

android {
    namespace = "com.example.kmpicture.domain"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}
