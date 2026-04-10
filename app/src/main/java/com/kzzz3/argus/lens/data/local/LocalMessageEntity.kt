package com.kzzz3.argus.lens.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_message",
    foreignKeys = [
        ForeignKey(
            entity = LocalConversationEntity::class,
            parentColumns = ["storageId"],
            childColumns = ["conversationStorageId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["accountId", "conversationId", "sortOrder"]),
        Index(value = ["conversationStorageId"]),
        Index(value = ["conversationId"]),
    ]
)
data class LocalMessageEntity(
    @PrimaryKey
    val storageId: String,
    val id: String,
    val accountId: String,
    val conversationId: String,
    val conversationStorageId: String,
    val senderDisplayName: String,
    val body: String,
    val timestampLabel: String,
    val isFromCurrentUser: Boolean,
    val deliveryStatus: String,
    val statusUpdatedAt: String,
    val sortOrder: Int,
)
