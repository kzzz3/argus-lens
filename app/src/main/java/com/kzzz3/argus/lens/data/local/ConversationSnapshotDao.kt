package com.kzzz3.argus.lens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationSnapshotDao {
    @Query("SELECT * FROM conversation_snapshot WHERE key = :key LIMIT 1")
    suspend fun findByKey(key: String): ConversationSnapshotEntity?

    @Query("DELETE FROM conversation_snapshot WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationSnapshotEntity)
}
