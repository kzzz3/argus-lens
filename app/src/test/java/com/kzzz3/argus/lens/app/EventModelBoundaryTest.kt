package com.kzzz3.argus.lens.app

import java.io.File
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
}
