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
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.exifinterface:exifinterface:1.3.7")
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
