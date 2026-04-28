plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kzzz3.argus.lens.core.data"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        debug {
            buildConfigField("String", "AUTH_MODE", "\"REMOTE\"")
            buildConfigField("String", "CONVERSATION_MODE", "\"REMOTE\"")
        }

        release {
            buildConfigField("String", "AUTH_MODE", "\"REMOTE\"")
            buildConfigField("String", "CONVERSATION_MODE", "\"REMOTE\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:session"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
