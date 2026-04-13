package com.kzzz3.argus.lens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationSnapshotEntity::class,
        LocalConversationEntity::class,
        LocalMessageEntity::class,
        LocalDraftAttachmentEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class ArgusLensDatabase : RoomDatabase() {
    abstract fun conversationSnapshotDao(): ConversationSnapshotDao
    abstract fun localConversationDao(): LocalConversationDao

    companion object {
        @Volatile
        private var INSTANCE: ArgusLensDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_conversation_accountId_sortOrder` ON `local_conversation` (`accountId`, `sortOrder`)"
                )
                db.execSQL(
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_message_accountId_conversationId_sortOrder` ON `local_message` (`accountId`, `conversationId`, `sortOrder`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_message_conversationStorageId` ON `local_message` (`conversationStorageId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_message_conversationId` ON `local_message` (`conversationId`)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_draft_attachment` (
                        `storageId` TEXT NOT NULL,
                        `id` TEXT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `conversationStorageId` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`storageId`),
                        FOREIGN KEY(`conversationStorageId`) REFERENCES `local_conversation`(`storageId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_draft_attachment_accountId_conversationStorageId_sortOrder` ON `local_draft_attachment` (`accountId`, `conversationStorageId`, `sortOrder`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_draft_attachment_conversationStorageId` ON `local_draft_attachment` (`conversationStorageId`)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `local_conversation` ADD COLUMN `syncCursor` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE `local_message` ADD COLUMN `statusUpdatedAt` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `local_message` ADD COLUMN `attachmentId` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `local_message` ADD COLUMN `attachmentType` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `local_message` ADD COLUMN `attachmentFileName` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `local_message` ADD COLUMN `attachmentContentType` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `local_message` ADD COLUMN `attachmentContentLength` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): ArgusLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = ArgusLensDatabase::class.java,
                    name = "argus-lens.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
        }
    }
}
