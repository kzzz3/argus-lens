package com.kzzz3.argus.lens.core.data.friend

import com.google.gson.Gson
import com.kzzz3.argus.lens.core.network.ApiErrorResponse
import com.kzzz3.argus.lens.core.network.friend.AddFriendRequestBody
import com.kzzz3.argus.lens.core.network.friend.FriendApiService
import com.kzzz3.argus.lens.core.network.friend.FriendRequestResponse
import com.kzzz3.argus.lens.core.network.friend.FriendResponse
import com.kzzz3.argus.lens.session.SessionRepository
import java.io.IOException

internal class RemoteFriendRepository(
    private val sessionRepository: SessionRepository,
    private val friendApiService: FriendApiService,
    private val gson: Gson = Gson(),
) : FriendRepository {
    override suspend fun listFriends(): FriendRepositoryResult {
        val token = sessionRepository.loadCredentials().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = friendApiService.listFriends("Bearer $token")
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                FriendRepositoryResult.FriendsSuccess(
                    friends = response.body().orEmpty().map { it.toEntry() }
                )
            }
        } catch (_: IOException) {
            FriendRepositoryResult.Failure("NETWORK_UNAVAILABLE", "Cannot reach friend service.")
        }
    }

    override suspend fun sendFriendRequest(friendAccountId: String): FriendRepositoryResult {
        val token = sessionRepository.loadCredentials().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = friendApiService.sendFriendRequest(
                authorizationHeader = "Bearer $token",
                request = AddFriendRequestBody(friendAccountId.trim()),
            )
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val created = response.body() ?: return FriendRepositoryResult.Failure("EMPTY_FRIEND_RESPONSE", "Friend service returned an empty response.")
                FriendRepositoryResult.FriendRequestSuccess(
                    request = created.toRequestEntry(),
                    message = "Friend request sent.",
                )
            }
        } catch (_: IOException) {
            FriendRepositoryResult.Failure("NETWORK_UNAVAILABLE", "Cannot reach friend service.")
        }
    }

    override suspend fun listFriendRequests(): FriendRepositoryResult {
        val token = sessionRepository.loadCredentials().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = friendApiService.listFriendRequests("Bearer $token")
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return FriendRepositoryResult.Failure("EMPTY_REQUESTS_RESPONSE", "Friend service returned an empty response.")
                FriendRepositoryResult.RequestsSuccess(
                    snapshot = FriendRequestsSnapshot(
                        incoming = body.incoming.map { it.toRequestEntry() },
                        outgoing = body.outgoing.map { it.toRequestEntry() },
                    ),
                )
            }
        } catch (_: IOException) {
            FriendRepositoryResult.Failure("NETWORK_UNAVAILABLE", "Cannot reach friend service.")
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): FriendRepositoryResult {
        val token = sessionRepository.loadCredentials().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = friendApiService.acceptFriendRequest(requestId.trim(), "Bearer $token")
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val accepted = response.body() ?: return FriendRepositoryResult.Failure("EMPTY_FRIEND_RESPONSE", "Friend service returned an empty response.")
                FriendRepositoryResult.FriendsSuccess(
                    friends = listOf(accepted.toEntry()),
                    message = "Friend request accepted.",
                )
            }
        } catch (_: IOException) {
            FriendRepositoryResult.Failure("NETWORK_UNAVAILABLE", "Cannot reach friend service.")
        }
    }

    override suspend fun rejectFriendRequest(requestId: String): FriendRepositoryResult {
        return resolveRequestMutation(
            requestId = requestId,
            mutation = { token -> friendApiService.rejectFriendRequest(requestId.trim(), "Bearer $token") },
            successMessage = "Friend request rejected.",
        )
    }

    override suspend fun ignoreFriendRequest(requestId: String): FriendRepositoryResult {
        return resolveRequestMutation(
            requestId = requestId,
            mutation = { token -> friendApiService.ignoreFriendRequest(requestId.trim(), "Bearer $token") },
            successMessage = "Friend request ignored.",
        )
    }

    private suspend fun resolveRequestMutation(
        requestId: String,
        mutation: suspend (String) -> retrofit2.Response<FriendRequestResponse>,
        successMessage: String,
    ): FriendRepositoryResult {
        val token = sessionRepository.loadCredentials().accessToken
        if (token.isBlank()) {
            return FriendRepositoryResult.Failure("INVALID_CREDENTIALS", "No active session token.")
        }

        return try {
            val response = mutation(token)
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val updated = response.body() ?: return FriendRepositoryResult.Failure("EMPTY_FRIEND_RESPONSE", "Friend service returned an empty response.")
                FriendRepositoryResult.FriendRequestSuccess(
                    request = updated.toRequestEntry(),
                    message = successMessage,
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

    private fun FriendRequestResponse.toRequestEntry(): FriendRequestEntry {
        return FriendRequestEntry(
            requestId = requestId,
            accountId = accountId,
            displayName = displayName,
            direction = direction,
            status = status,
            note = note,
        )
    }
}
