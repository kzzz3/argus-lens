package com.kzzz3.argus.lens.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_snapshot")
data class ConversationSnapshotEntity(
    @PrimaryKey
    val key: String,
    val payloadJson: String,
)
