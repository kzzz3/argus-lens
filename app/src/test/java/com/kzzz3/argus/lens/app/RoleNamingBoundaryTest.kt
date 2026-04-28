package com.kzzz3.argus.lens.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleNamingBoundaryTest {
    @Test
    fun roleNamingTaxonomyDocumentsEveryAppRole() {
        val architectureTarget = File("../docs/android-architecture-target.md").readText()

        listOf(
            "## Role Naming Boundary",
            "UseCase",
            "Manager",
            "Scheduler",
            "Handler",
            "Runner",
            "Store",
            "StateHolder",
            "Controller",
        ).forEach { expectedTerm ->
            assertTrue(
                "Architecture target must document the role naming term $expectedTerm",
                architectureTarget.contains(expectedTerm),
            )
        }

        listOf(
            "AppRouteNavigator",
            "PersistAppStateUseCase",
            "AppRouteHandlers",
            "CallSessionTimer",
            "RealtimeConnectionManager",
            "RealtimeReconnectScheduler",
            "SessionCredentialsStore",
            "LocalSessionStore",
        ).forEach { transitionalName ->
            assertTrue(
                "Architecture target must classify transitional role name $transitionalName",
                architectureTarget.contains(transitionalName),
            )
        }
    }

    @Test
    fun appRouteHandlersDoNotConstructFeatureOwnedStateHolders() {
        val routeHandlersSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppRouteHandlers.kt").readText()

        listOf("StateHolder(", "ActionHandler(", "EffectHandler(").forEach { forbiddenSource ->
            assertFalse(
                "AppRouteHandlers.kt must not construct feature-owned role $forbiddenSource",
                routeHandlersSource.contains(forbiddenSource),
            )
        }
    }

    @Test
    fun featureStateHoldersDoNotOwnNavigationControllers() {
        listOf(
            "../feature/src/main/java/com/kzzz3/argus/lens/feature/auth/AuthStateHolder.kt",
            "../feature/src/main/java/com/kzzz3/argus/lens/feature/inbox/ChatStateHolder.kt",
            "../feature/src/main/java/com/kzzz3/argus/lens/feature/inbox/InboxStateHolder.kt",
            "../feature/src/main/java/com/kzzz3/argus/lens/feature/wallet/WalletStateHolder.kt",
        ).forEach { relativePath ->
            val source = File(relativePath).readText()

            listOf("NavController", "rememberNavController", "androidx.navigation").forEach { forbiddenSource ->
                assertFalse(
                    "$relativePath must not own navigation API $forbiddenSource",
                    source.contains(forbiddenSource),
                )
            }
        }
    }
}
