package com.kzzz3.argus.lens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalConversationEntity::class,
        LocalMessageEntity::class,
        LocalDraftAttachmentEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ArgusLensDatabase : RoomDatabase() {
    abstract fun localConversationDao(): LocalConversationDao

    companion object {
        @Volatile
        private var INSTANCE: ArgusLensDatabase? = null

        fun getInstance(context: Context): ArgusLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = ArgusLensDatabase::class.java,
                    name = "argus-lens-v1.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
