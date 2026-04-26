package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.model.session.AppSessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class ArgusLensAppViewModelTest {
    @Test
    fun appViewModelUsesInjectedDependenciesWithoutApplicationSuperclass() {
        assertEquals(ViewModel::class.java, ArgusLensAppViewModel::class.java.superclass)
    }

    @Test
    fun resolveInitialAppRoute_authenticatedSessionWithTokenStartsInInbox() {
        val route = resolveInitialAppRoute(
            session = AppSessionState(
                isAuthenticated = true,
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            credentials = SessionCredentials(accessToken = "access-token"),
        )

        assertEquals(AppRoute.Inbox, route)
    }

    @Test
    fun resolveInitialAppRoute_missingTokenStartsAtAuthEntry() {
        val route = resolveInitialAppRoute(
            session = AppSessionState(
                isAuthenticated = true,
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            credentials = SessionCredentials(),
        )

        assertEquals(AppRoute.AuthEntry, route)
    }
}
