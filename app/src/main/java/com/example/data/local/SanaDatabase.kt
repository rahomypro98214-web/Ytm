package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, ChatConversationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SanaDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatConversationDao(): ChatConversationDao

    companion object {
        @Volatile
        private var INSTANCE: SanaDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT '$DEFAULT_CONVERSATION_ID'"
                )
                // Drop any legacy indices from earlier v1 builds so Room schema validation succeeds cleanly.
                val legacyIndices = mutableListOf<String>()
                db.query("PRAGMA index_list('chat_messages')").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    val originIdx = cursor.getColumnIndex("origin")
                    while (cursor.moveToNext()) {
                        val idxName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        val origin = if (originIdx >= 0) cursor.getString(originIdx) else "c"
                        if (!idxName.isNullOrBlank() &&
                            !idxName.startsWith("sqlite_autoindex_") &&
                            origin != "pk" &&
                            idxName != "index_chat_messages_userId_conversationId"
                        ) {
                            legacyIndices.add(idxName)
                        }
                    }
                }
                legacyIndices.forEach { oldIndex ->
                    val escaped = oldIndex.replace("\"", "\"\"")
                    db.execSQL("DROP INDEX IF EXISTS \"$escaped\"")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_messages_userId_conversationId " +
                        "ON chat_messages (userId, conversationId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        conversationId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        previewText TEXT NOT NULL,
                        messageCount INTEGER NOT NULL,
                        isSaved INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(userId, conversationId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_conversations_userId_updatedAtMillis " +
                        "ON chat_conversations (userId, updatedAtMillis)"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO chat_conversations (
                        conversationId, userId, title, previewText, messageCount, isSaved, createdAtMillis, updatedAtMillis
                    )
                    SELECT
                        '$DEFAULT_CONVERSATION_ID',
                        userId,
                        'محادثة سابقة',
                        COALESCE((SELECT m2.text FROM chat_messages m2 WHERE m2.userId = m1.userId ORDER BY m2.createdAtMillis DESC, m2.localId DESC LIMIT 1), ''),
                        COUNT(*),
                        1,
                        MIN(createdAtMillis),
                        MAX(createdAtMillis)
                    FROM chat_messages m1
                    GROUP BY userId
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SanaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SanaDatabase::class.java,
                    "sana_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
