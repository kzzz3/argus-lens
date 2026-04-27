package com.kzzz3.argus.lens.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventModelBoundaryTest {
    @Test
    fun featureOneOffEffectsUseSealedEffectTypes() {
        val effectFiles = File("../feature/src/main/java/com/kzzz3/argus/lens/feature")
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith("Effect.kt") }
            .toList()

        assertTrue("Expected feature effect files for one-off events.", effectFiles.isNotEmpty())
        effectFiles.forEach { effectFile ->
            val source = effectFile.readText()
            assertTrue(
                "${effectFile.invariantSeparatorsPath} must declare a sealed effect model.",
                source.contains("sealed interface") || source.contains("sealed class"),
            )
        }
    }

    @Test
    fun eventMessageMechanismDocumentsStateEffectAndRootDisplayBoundaries() {
        val progress = File("../docs/android-modernization-progress.md").readText()
        val architectureTarget = File("../docs/android-architecture-target.md").readText()

        assertTrue(
            "Modernization progress must define screen-scoped message ownership.",
            progress.contains("Screen-scoped user-visible messages stay feature-owned"),
        )
        assertTrue(
            "Modernization progress must keep one-off work on sealed effects.",
            progress.contains("One-off commands remain sealed feature `*Effect` models"),
        )
        assertTrue(
            "Modernization progress must defer root snackbar work until app-wide producers justify it.",
            progress.contains("Root snackbar rendering remains deferred until multiple independent app-wide producers need one visual queue"),
        )
        assertTrue(
            "Architecture target must define UiStatusMessage as the shared screen-message primitive.",
            architectureTarget.contains("Screen-scoped messages stay feature-owned and may use `UiStatusMessage`"),
        )
        assertTrue(
            "Architecture target must document that snackbar/global bus primitives are not the default event model.",
            architectureTarget.contains("`SnackbarHostState`, `SharedFlow`, `Channel`, and `Toast` are not default event mechanisms"),
        )
    }

    @Test
    fun appAndFeatureSourcesDoNotIntroduceGlobalMessageBusPrimitives() {
        val sourceRoots = listOf(
            File("../app/src/main/java/com/kzzz3/argus/lens"),
            File("../feature/src/main/java/com/kzzz3/argus/lens/feature"),
        )
        val forbiddenPrimitives = listOf(
            "MutableSharedFlow",
            "SharedFlow<",
            "kotlinx.coroutines.flow.SharedFlow",
            "Channel<",
            "Channel(",
            "kotlinx.coroutines.channels.Channel",
            "SnackbarHostState",
            "SnackbarHost(",
            "Toast.makeText",
        )

        val sourceFiles = sourceRoots
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

        assertTrue("Expected app and feature Kotlin sources to scan.", sourceFiles.isNotEmpty())
        sourceFiles.forEach { sourceFile ->
            val source = sourceFile.readText()
            forbiddenPrimitives.forEach { primitive ->
                assertFalse(
                    "${sourceFile.invariantSeparatorsPath} must not introduce $primitive before the root message-display threshold is met.",
                    source.contains(primitive),
                )
            }
        }
    }
}
