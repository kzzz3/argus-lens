package com.kzzz3.argus.lens.data.auth

class LocalAuthRepository : AuthRepository {
    override suspend fun restoreSession(accessToken: String): AuthRepositoryResult {
        val normalizedAccountId = accessToken.removePrefix("local-token-").ifBlank { "argus-user" }
        return AuthRepositoryResult.Success(
            session = AuthSession(
                accountId = normalizedAccountId,
                displayName = normalizedAccountId,
                accessToken = accessToken,
                refreshToken = createLocalRefreshToken(normalizedAccountId),
                message = "Local session restored.",
            )
        )
    }

    override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult {
        val normalizedAccountId = refreshToken.removePrefix("local-refresh-token-").ifBlank { "argus-user" }
        return AuthRepositoryResult.Success(
            session = AuthSession(
                accountId = normalizedAccountId,
                displayName = normalizedAccountId,
                accessToken = createLocalAccessToken(normalizedAccountId),
                refreshToken = createLocalRefreshToken(normalizedAccountId),
                message = "Local access token refreshed.",
            )
        )
    }

    override suspend fun login(account: String, password: String): AuthRepositoryResult {
        val trimmedAccount = account.trim()
        val resolvedDisplayName = trimmedAccount.ifEmpty { "Argus User" }

        return AuthRepositoryResult.Success(
            session = AuthSession(
                accountId = trimmedAccount,
                displayName = resolvedDisplayName,
                accessToken = createLocalAccessToken(trimmedAccount),
                refreshToken = createLocalRefreshToken(trimmedAccount),
                message = "Local preview sign-in complete.",
            )
        )
    }

    override suspend fun register(
        displayName: String,
        account: String,
        password: String,
    ): AuthRepositoryResult {
        val trimmedAccount = account.trim()
        val resolvedDisplayName = displayName.trim().ifEmpty { trimmedAccount.ifEmpty { "Argus User" } }

        return AuthRepositoryResult.Success(
            session = AuthSession(
                accountId = trimmedAccount,
                displayName = resolvedDisplayName,
                accessToken = createLocalAccessToken(trimmedAccount),
                refreshToken = createLocalRefreshToken(trimmedAccount),
                message = "Local preview registration complete.",
            )
        )
    }
}

private fun createLocalAccessToken(
    accountId: String,
): String {
    val normalizedAccountId = accountId.trim().ifEmpty { "argus-user" }
    return "local-token-$normalizedAccountId"
}

private fun createLocalRefreshToken(
    accountId: String,
): String {
    val normalizedAccountId = accountId.trim().ifEmpty { "argus-user" }
    return "local-refresh-token-$normalizedAccountId"
}
