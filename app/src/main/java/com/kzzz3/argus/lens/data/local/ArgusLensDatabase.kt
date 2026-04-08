package com.kzzz3.argus.lens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationSnapshotEntity::class, LocalConversationEntity::class, LocalMessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ArgusLensDatabase : RoomDatabase() {
    abstract fun conversationSnapshotDao(): ConversationSnapshotDao
    abstract fun localConversationDao(): LocalConversationDao

    companion object {
        @Volatile
        private var INSTANCE: ArgusLensDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_conversation` (
                        `storageId` TEXT NOT NULL,
                        `id` TEXT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `unreadCount` INTEGER NOT NULL,
                        `draftMessage` TEXT NOT NULL,
                        `draftAttachmentsJson` TEXT NOT NULL,
                        `isVoiceRecording` INTEGER NOT NULL,
                        `voiceRecordingSeconds` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`storageId`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_conversation_accountId_sortOrder` ON `local_conversation` (`accountId`, `sortOrder`)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_message` (
                        `storageId` TEXT NOT NULL,
                        `id` TEXT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `conversationStorageId` TEXT NOT NULL,
                        `senderDisplayName` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `timestampLabel` TEXT NOT NULL,
                        `isFromCurrentUser` INTEGER NOT NULL,
                        `deliveryStatus` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`storageId`),
                        FOREIGN KEY(`conversationStorageId`) REFERENCES `local_conversation`(`storageId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_message_accountId_conversationId_sortOrder` ON `local_message` (`accountId`, `conversationId`, `sortOrder`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_message_conversationId` ON `local_message` (`conversationStorageId`)"
                )
            }
        }

        fun getInstance(context: Context): ArgusLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = ArgusLensDatabase::class.java,
                    name = "argus-lens.db",
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
