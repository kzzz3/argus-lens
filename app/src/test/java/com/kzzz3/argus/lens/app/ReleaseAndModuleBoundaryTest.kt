package com.kzzz3.argus.lens.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAndModuleBoundaryTest {
    @Test
    fun releaseBuildTypeEnablesMinifyAndResourceShrinking() {
        val buildFile = File("build.gradle.kts").readText()

        assertTrue(buildFile.contains("release {"))
        assertTrue(buildFile.contains("isMinifyEnabled = true"))
        assertTrue(buildFile.contains("isShrinkResources = true"))
        assertTrue(buildFile.contains("proguard-android-optimize.txt"))
        assertTrue(buildFile.contains("ARGUS_RELEASE_BASE_URL is required for release builds."))
    }

    @Test
    fun moduleGraphKeepsProductBoundariesExplicit() {
        val settings = File("../settings.gradle.kts").readText()
        val appBuildFile = File("build.gradle.kts").readText()

        listOf(":app", ":data", ":feature", ":model", ":ui").forEach { moduleName ->
            assertTrue(settings.contains("include(\"$moduleName\")"))
        }
        listOf(":data", ":feature", ":model", ":ui").forEach { dependencyName ->
            assertTrue(appBuildFile.contains("implementation(project(\"$dependencyName\"))"))
        }
    }
}
