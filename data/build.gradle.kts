import java.net.URI

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

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

android {
    namespace = "com.kzzz3.argus.lens.data"
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
            buildConfigField("String", "AUTH_BASE_URL", "\"$debugAuthBaseUrl\"")
        }

        release {
            buildConfigField("String", "AUTH_MODE", "\"REMOTE\"")
            buildConfigField("String", "CONVERSATION_MODE", "\"REMOTE\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"$releaseAuthBaseUrl\"")
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(project(":model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
}
