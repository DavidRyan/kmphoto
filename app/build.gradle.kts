plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kmpicture"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
    }
}

android {
    namespace = "com.example.kmpicture.app"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}
