package com.kzzz3.argus.lens.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_conversation",
    indices = [Index(value = ["accountId", "sortOrder"])]
)
data class LocalConversationEntity(
    @PrimaryKey
    val storageId: String,
    val id: String,
    val accountId: String,
    val title: String,
    val subtitle: String,
    val unreadCount: Int,
    val syncCursor: String,
    val draftMessage: String,
    val isVoiceRecording: Boolean,
    val voiceRecordingSeconds: Int,
    val sortOrder: Int,
)
