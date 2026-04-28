package com.kzzz3.argus.lens.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAndModuleBoundaryTest {
    @Test
    fun releaseBuildTypeEnablesMinifyAndResourceShrinking() {
        val buildFile = File("build.gradle.kts").readText()
        val networkBuildFile = File("../core/network/build.gradle.kts").readText()

        assertTrue(buildFile.contains("release {"))
        assertTrue(buildFile.contains("isMinifyEnabled = true"))
        assertTrue(buildFile.contains("isShrinkResources = true"))
        assertTrue(buildFile.contains("proguard-android-optimize.txt"))
        assertTrue(networkBuildFile.contains("ARGUS_RELEASE_BASE_URL is required for release builds."))
    }

    @Test
    fun moduleGraphKeepsProductBoundariesExplicit() {
        val settings = File("../settings.gradle.kts").readText()
        val appBuildFile = File("build.gradle.kts").readText()

        listOf(
            ":app",
            ":feature",
            ":core:data",
            ":core:network",
            ":core:database",
            ":core:datastore",
            ":core:model",
            ":core:session",
            ":core:ui",
        ).forEach { moduleName ->
            assertTrue(settings.contains("include(\"$moduleName\")"))
        }
        listOf(":feature", ":core:data", ":core:model", ":core:session", ":core:ui").forEach { dependencyName ->
            assertTrue(appBuildFile.contains("implementation(project(\"$dependencyName\"))"))
        }
        listOf(":core:network", ":core:datastore").forEach { lowLevelDependency ->
            assertTrue("App must not directly depend on low-level data implementation module: $lowLevelDependency", !appBuildFile.contains("implementation(project(\"$lowLevelDependency\"))"))
        }
    }

    @Test
    fun coreSessionContractIsPhysicallyExtractedBeforeStateStoreMigration() {
        val settings = File("../settings.gradle.kts").readText()
        val dataBuildFile = File("../core/data/build.gradle.kts").readText()
        val coreSessionBuildFile = File("../core/session/build.gradle.kts").readText()
        val coreSessionContract = File("../core/session/src/main/java/com/kzzz3/argus/lens/session/SessionRepository.kt").readText()
        val oldCoreDataContract = File("../core/data/src/main/java/com/kzzz3/argus/lens/core/data/session/SessionRepository.kt")

        assertTrue(settings.contains("include(\":core:session\")"))
        assertTrue(dataBuildFile.contains("implementation(project(\":core:session\"))"))
        assertTrue(coreSessionBuildFile.contains("namespace = \"com.kzzz3.argus.lens.session\""))
        assertTrue(coreSessionContract.contains("package com.kzzz3.argus.lens.session"))
        assertTrue(coreSessionContract.contains("interface SessionRepository"))
        assertTrue(coreSessionContract.contains("data class SessionCredentials"))
        assertTrue("core data must not retain the canonical session contract after extraction.", !oldCoreDataContract.exists())
    }

    @Test
    fun moduleSplitReadinessKeepsRemainingFutureModulesPlannedButNotIncluded() {
        val settings = File("../settings.gradle.kts").readText()
        val architectureTarget = File("../docs/android-architecture-target.md").readText()
        val progress = File("../docs/android-modernization-progress.md").readText()

        listOf(
            ":feature:auth",
            ":feature:inbox",
            ":feature:chat",
            ":feature:wallet",
        ).forEach { futureModule ->
            assertTrue("Future module must not be included before readiness is proven: $futureModule", !settings.contains("include(\"$futureModule\")"))
            assertTrue("Architecture target must document future module readiness: $futureModule", architectureTarget.contains("`$futureModule`"))
        }

        assertTrue(architectureTarget.contains("## Module Split Readiness Plan"))
        assertTrue(architectureTarget.contains("Repository orchestration lives in `:core:data`"))
        assertTrue(architectureTarget.contains("Do not introduce `buildSrc` or included `build-logic` until a dedicated convention-plugin slice."))
        assertTrue(progress.contains("## P2 Module Split And Gradle Conventions"))
        assertTrue(progress.contains("Status: complete for the current core data-layer split."))
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
