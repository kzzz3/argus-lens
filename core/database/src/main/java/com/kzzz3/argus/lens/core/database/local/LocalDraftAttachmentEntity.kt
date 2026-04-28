package com.kzzz3.argus.lens.core.database.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_draft_attachment",
    foreignKeys = [
        ForeignKey(
            entity = LocalConversationEntity::class,
            parentColumns = ["storageId"],
            childColumns = ["conversationStorageId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["accountId", "conversationStorageId", "sortOrder"]),
        Index(value = ["conversationStorageId"]),
    ]
)
data class LocalDraftAttachmentEntity(
    @PrimaryKey
    val storageId: String,
    val id: String,
    val accountId: String,
    val conversationId: String,
    val conversationStorageId: String,
    val kind: String,
    val title: String,
    val summary: String,
    val sortOrder: Int,
)
