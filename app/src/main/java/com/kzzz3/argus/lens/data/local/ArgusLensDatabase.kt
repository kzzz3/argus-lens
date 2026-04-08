package com.kzzz3.argus.lens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationSnapshotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ArgusLensDatabase : RoomDatabase() {
    abstract fun conversationSnapshotDao(): ConversationSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: ArgusLensDatabase? = null

        fun getInstance(context: Context): ArgusLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = ArgusLensDatabase::class.java,
                    name = "argus-lens.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
