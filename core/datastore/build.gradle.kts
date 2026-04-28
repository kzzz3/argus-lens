plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kzzz3.argus.lens.core.datastore"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:session"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
