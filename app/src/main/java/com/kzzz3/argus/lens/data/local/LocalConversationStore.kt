package com.kzzz3.argus.lens.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.inbox.createInboxSampleThreads

class LocalConversationStore(
    private val dao: ConversationSnapshotDao,
    private val gson: Gson,
) {
    suspend fun loadConversationThreads(accountId: String): List<InboxConversationThread>? {
        val entity = dao.findByKey(conversationSnapshotKey(accountId)) ?: return null
        val type = object : TypeToken<List<InboxConversationThread>>() {}.type
        return gson.fromJson(entity.payloadJson, type)
    }

    suspend fun saveConversationThreads(
        accountId: String,
        threads: List<InboxConversationThread>,
    ) {
        dao.upsert(
            ConversationSnapshotEntity(
                key = conversationSnapshotKey(accountId),
                payloadJson = gson.toJson(threads),
            )
        )
    }

    suspend fun clearConversationThreads(accountId: String) {
        dao.deleteByKey(conversationSnapshotKey(accountId))
    }
}

class LocalConversationCoordinator(
    private val store: LocalConversationStore,
) {
    fun createPreviewState(
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        return ConversationThreadsState(
            threads = createInboxSampleThreads(currentUserDisplayName = currentUserDisplayName),
        )
    }

    suspend fun loadOrCreateConversationThreads(
        accountId: String,
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        val storedThreads = store.loadConversationThreads(accountId)
        if (!storedThreads.isNullOrEmpty()) {
            return ConversationThreadsState(storedThreads)
        }

        val seededState = createPreviewState(currentUserDisplayName)
        store.saveConversationThreads(accountId, seededState.threads)
        return seededState
    }

    suspend fun saveConversationThreads(
        accountId: String,
        state: ConversationThreadsState,
    ) {
        if (accountId.isBlank()) return
        store.saveConversationThreads(accountId, state.threads)
    }

    suspend fun clearConversationThreads(accountId: String) {
        if (accountId.isBlank()) return
        store.clearConversationThreads(accountId)
    }
}

fun createLocalConversationStore(
    context: Context,
): LocalConversationStore {
    val database = ArgusLensDatabase.getInstance(context)
    return LocalConversationStore(
        dao = database.conversationSnapshotDao(),
        gson = Gson(),
    )
}

fun createLocalConversationCoordinator(
    context: Context,
): LocalConversationCoordinator {
    return LocalConversationCoordinator(createLocalConversationStore(context))
}

private fun conversationSnapshotKey(accountId: String): String {
    val normalizedAccountId = accountId.trim().ifEmpty { "anonymous" }
    return "local-conversation-threads:$normalizedAccountId"
}
