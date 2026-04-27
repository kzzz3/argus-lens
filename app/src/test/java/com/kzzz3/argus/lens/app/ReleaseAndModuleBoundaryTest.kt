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

        listOf(":app", ":data", ":feature", ":core:model", ":core:ui").forEach { moduleName ->
            assertTrue(settings.contains("include(\"$moduleName\")"))
        }
        listOf(":data", ":feature", ":core:model", ":core:ui").forEach { dependencyName ->
            assertTrue(appBuildFile.contains("implementation(project(\"$dependencyName\"))"))
        }
    }

    @Test
    fun moduleSplitReadinessKeepsFutureModulesPlannedButNotIncluded() {
        val settings = File("../settings.gradle.kts").readText()
        val architectureTarget = File("../docs/android-architecture-target.md").readText()
        val progress = File("../docs/android-modernization-progress.md").readText()

        listOf(
            ":core:session",
            ":core:network",
            ":core:database",
            ":feature:auth",
            ":feature:inbox",
            ":feature:chat",
            ":feature:wallet",
        ).forEach { futureModule ->
            assertTrue("Future module must not be included before readiness is proven: $futureModule", !settings.contains("include(\"$futureModule\")"))
            assertTrue("Architecture target must document future module readiness: $futureModule", architectureTarget.contains("`$futureModule`"))
        }

        assertTrue(architectureTarget.contains("## Module Split Readiness Plan"))
        assertTrue(architectureTarget.contains("First physical split candidate: `:core:network`"))
        assertTrue(architectureTarget.contains("Do not introduce `buildSrc` or included `build-logic` until a dedicated convention-plugin slice."))
        assertTrue(progress.contains("## P2 Module Split And Gradle Conventions"))
        assertTrue(progress.contains("Status: complete for the readiness plan; no physical modules or convention plugins were added."))
    }

    @Test
    fun gradleConventionsStayCentralizedUntilBuildLogicSlice() {
        val settings = File("../settings.gradle.kts").readText()
        val versionCatalog = File("../gradle/libs.versions.toml")

        assertTrue(settings.contains("RepositoriesMode.FAIL_ON_PROJECT_REPOS"))
        assertTrue(versionCatalog.isFile)
        assertTrue("buildSrc must not be introduced before the convention-plugin slice.", !File("../buildSrc").exists())
        assertTrue("Included build-logic must not be introduced before the convention-plugin slice.", !File("../build-logic").exists())
    }
}
