package com.kzzz3.argus.lens.data.friend

import com.google.gson.Gson
import com.kzzz3.argus.lens.data.auth.ApiErrorResponse
import com.kzzz3.argus.lens.data.session.SessionRepository
import java.io.IOException

class RemoteFriendRepository(
    private val sessionRepository: SessionRepository,
    private val friendApiService: FriendApiService,
    private val gson: Gson = Gson(),
) : FriendRepository {
    override suspend fun listFriends(): FriendRepositoryResult {
        val token = sessionRepository.loadSession().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = friendApiService.listFriends("Bearer $token")
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                FriendRepositoryResult.Success(
                    friends = response.body().orEmpty().map { it.toEntry() }
                )
            }
        } catch (_: IOException) {
            FriendRepositoryResult.Failure("NETWORK_UNAVAILABLE", "Cannot reach friend service.")
        }
    }

    override suspend fun addFriend(friendAccountId: String): FriendRepositoryResult {
        val token = sessionRepository.loadSession().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = friendApiService.addFriend(
                authorizationHeader = "Bearer $token",
                request = AddFriendRequestBody(friendAccountId.trim()),
            )
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val created = response.body() ?: return FriendRepositoryResult.Failure("EMPTY_FRIEND_RESPONSE", "Friend service returned an empty response.")
                FriendRepositoryResult.Success(
                    friends = listOf(created.toEntry()),
                    message = "Friend added successfully.",
                )
            }
        } catch (_: IOException) {
            FriendRepositoryResult.Failure("NETWORK_UNAVAILABLE", "Cannot reach friend service.")
        }
    }

    private fun parseFailure(httpCode: Int, rawBody: String): FriendRepositoryResult.Failure {
        val parsed = runCatching { gson.fromJson(rawBody, ApiErrorResponse::class.java) }.getOrNull()
        return FriendRepositoryResult.Failure(
            code = parsed?.code,
            message = parsed?.message ?: "Friend request failed with HTTP $httpCode.",
        )
    }

    private fun FriendResponse.toEntry(): FriendEntry {
        return FriendEntry(accountId = accountId, displayName = displayName, note = note)
    }
}
