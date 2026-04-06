package com.kzzz3.argus.lens.data.auth

class LocalAuthRepository : AuthRepository {
    override suspend fun login(account: String, password: String): AuthRepositoryResult {
        val trimmedAccount = account.trim()
        val resolvedDisplayName = trimmedAccount.ifEmpty { "Argus User" }

        return AuthRepositoryResult.Success(
            session = AuthSession(
                accountId = trimmedAccount,
                displayName = resolvedDisplayName,
                accessToken = createLocalAccessToken(trimmedAccount),
                message = "Local login success. Network auth is temporarily bypassed.",
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
                message = "Local registration success. Network auth is temporarily bypassed.",
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
