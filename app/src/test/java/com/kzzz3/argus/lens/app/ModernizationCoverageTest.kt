package com.kzzz3.argus.lens.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernizationCoverageTest {
    @Test
    fun modernizationRegressionGatesExistForEveryPriorityGroup() {
        listOf(
            "src/test/java/com/kzzz3/argus/lens/app/SessionBoundaryTest.kt",
            "src/test/java/com/kzzz3/argus/lens/app/AppRouteNavigationRuntimeTest.kt",
            "src/test/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModelTest.kt",
            "../data/src/test/java/com/kzzz3/argus/lens/data/local/ArgusLensDatabaseMigrationTest.kt",
            "src/test/java/com/kzzz3/argus/lens/worker/BackgroundSyncWorkTest.kt",
            "src/test/java/com/kzzz3/argus/lens/worker/BackgroundSyncTaskTest.kt",
            "src/test/java/com/kzzz3/argus/lens/worker/BackgroundSyncWorkerTest.kt",
            "src/test/java/com/kzzz3/argus/lens/app/ReleaseAndModuleBoundaryTest.kt",
            "src/test/java/com/kzzz3/argus/lens/app/EventModelBoundaryTest.kt",
            "src/test/java/com/kzzz3/argus/lens/app/RoleNamingBoundaryTest.kt",
        ).forEach { relativePath ->
            assertTrue("Missing modernization regression gate: $relativePath", File(relativePath).isFile)
        }
    }

    @Test
    fun modernizationProgressDocumentTracksPriorityGroups() {
        val document = File("../docs/android-modernization-progress.md").readText()

        listOf("P0", "P1", "P2", "P3").forEach { priority ->
            assertTrue("Progress document must track $priority work.", document.contains("## $priority"))
        }
    }
}
