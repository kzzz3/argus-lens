package com.kzzz3.argus.lens.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceFactoryTest {

    @Test
    fun resolveAuthModeOrNull_returnsRemoteForRemoteBuildValue() {
        val result = resolveAuthModeOrNull("REMOTE")

        assertEquals(AuthMode.REMOTE, result)
    }

    @Test
    fun resolveAuthModeOrNull_returnsNullForUnknownBuildValue() {
        val result = resolveAuthModeOrNull("BROKEN_MODE")

        assertEquals(null, result)
    }

    @Test
    fun createAuthRepository_returnsFailureRepositoryWhenModeIsMissing() {
        val repository = createAuthRepositoryOrUnavailable(mode = null)

        assertTrue(repository is UnavailableAuthRepository)
    }
}
