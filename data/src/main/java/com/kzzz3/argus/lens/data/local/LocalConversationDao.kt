package com.kzzz3.argus.lens.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

data class LocalConversationWithMessages(
    @Embedded
    val conversation: LocalConversationEntity,
    @Relation(
        parentColumn = "storageId",
        entityColumn = "conversationStorageId",
        entity = LocalMessageEntity::class,
    )
    val messages: List<LocalMessageEntity>,
    @Relation(
        parentColumn = "storageId",
        entityColumn = "conversationStorageId",
        entity = LocalDraftAttachmentEntity::class,
    )
    val draftAttachments: List<LocalDraftAttachmentEntity>,
)

@Dao
interface LocalConversationDao {
    @Transaction
    @Query("SELECT * FROM local_conversation WHERE accountId = :accountId ORDER BY sortOrder ASC")
    suspend fun getConversationsWithMessages(accountId: String): List<LocalConversationWithMessages>

    @Query("DELETE FROM local_message WHERE accountId = :accountId")
    suspend fun deleteMessagesForAccount(accountId: String)

    @Query("DELETE FROM local_conversation WHERE accountId = :accountId")
    suspend fun deleteConversationsForAccount(accountId: String)

    @Query("DELETE FROM local_draft_attachment WHERE accountId = :accountId")
    suspend fun deleteDraftAttachmentsForAccount(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(entities: List<LocalConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(entities: List<LocalMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraftAttachments(entities: List<LocalDraftAttachmentEntity>)

    @Transaction
    suspend fun replaceAccountSnapshot(
        accountId: String,
        conversations: List<LocalConversationEntity>,
        messages: List<LocalMessageEntity>,
        draftAttachments: List<LocalDraftAttachmentEntity>,
    ) {
        deleteMessagesForAccount(accountId)
        deleteDraftAttachmentsForAccount(accountId)
        deleteConversationsForAccount(accountId)
        if (conversations.isNotEmpty()) {
            upsertConversations(conversations)
        }
        if (messages.isNotEmpty()) {
            upsertMessages(messages)
        }
        if (draftAttachments.isNotEmpty()) {
            upsertDraftAttachments(draftAttachments)
        }
    }
}
