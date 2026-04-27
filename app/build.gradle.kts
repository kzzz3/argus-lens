import java.net.URI

val debugAuthBaseUrl = "http://10.0.2.2:8080/"
fun isReleaseBuildTask(taskName: String): Boolean {
    val normalizedName = taskName.substringAfterLast(':').lowercase()
    val releaseTaskPrefixes = listOf("assemble", "bundle", "package", "lintvital")
    return normalizedName in setOf("assemble", "bundle", "build") ||
        releaseTaskPrefixes.any { prefix -> normalizedName.startsWith(prefix) && normalizedName.contains("release") }
}

val isReleaseBuildRequested = gradle.startParameter.taskNames.any(::isReleaseBuildTask)
fun validateReleaseBaseUrl(rawUrl: String): String {
    val uri = URI(rawUrl.trim())
    val host = uri.host?.lowercase()
        ?: error("ARGUS_RELEASE_BASE_URL must include a host.")
    val forbiddenHosts = setOf("localhost", "127.0.0.1", "0.0.0.0", "10.0.2.2", "debug-placeholder.invalid")
    require(uri.scheme == "https") { "ARGUS_RELEASE_BASE_URL must use HTTPS for release builds." }
    require(host !in forbiddenHosts) { "ARGUS_RELEASE_BASE_URL must not target localhost, emulator, or debug placeholder hosts." }
    require(uri.userInfo == null) { "ARGUS_RELEASE_BASE_URL must not include credentials." }
    return rawUrl.trim()
}

val releaseAuthBaseUrl = providers.gradleProperty("ARGUS_RELEASE_BASE_URL").orNull
    ?.let(::validateReleaseBaseUrl)
    ?: if (isReleaseBuildRequested) {
        error("ARGUS_RELEASE_BASE_URL is required for release builds.")
    } else {
        "https://debug-placeholder.invalid/"
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)

    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.kzzz3.argus.lens"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.kzzz3.argus.lens"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "AUTH_MODE", "\"REMOTE\"")
            buildConfigField("String", "CONVERSATION_MODE", "\"REMOTE\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"$debugAuthBaseUrl\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "AUTH_MODE", "\"REMOTE\"")
            buildConfigField("String", "CONVERSATION_MODE", "\"REMOTE\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"$releaseAuthBaseUrl\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.incremental", "true")
}

dependencies {
    implementation(project(":data"))
    implementation(project(":feature"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.hilt.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
