package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DEFAULT_CONVERSATION_ID
import com.example.data.local.SanaDatabase
import com.example.data.repository.LocalChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.theme.SanaTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: SanaDatabase
    private lateinit var repo: LocalChatRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.filesDir, "test_conversation_history.db")
        if (dbFile.exists()) dbFile.delete()
        db = Room.databaseBuilder(context, SanaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        repo = LocalChatRepository(db.chatMessageDao(), db.chatConversationDao())
    }

    @After
    fun tearDown() {
        if (db.isOpen) {
            db.close()
        }
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun savedConversationsAndMessagesPersistAcrossDatabaseReopen() = runBlocking {
        repo.insertMessage(
            userId = "local",
            role = "user",
            text = "اشرح لي برمجة Kotlin",
            createdAtMillis = 1000L,
            conversationId = DEFAULT_CONVERSATION_ID
        )
        repo.insertMessage(
            userId = "local",
            role = "model",
            text = "كوتلن لغة حديثة وآمنة لتطوير أندرويد.",
            createdAtMillis = 2000L,
            conversationId = DEFAULT_CONVERSATION_ID
        )

        val savedId = repo.saveConversation(
            userId = "local",
            sourceConversationId = DEFAULT_CONVERSATION_ID,
            customTitle = "خطة تعلم الكوتلن"
        )
        assertNotEquals(DEFAULT_CONVERSATION_ID, savedId)

        val laterBase = System.currentTimeMillis() + 10_000L
        val secondConvId = repo.createNewConversationId()
        repo.insertMessage(
            userId = "local",
            role = "user",
            text = "ما عاصمة المغرب؟",
            createdAtMillis = laterBase,
            conversationId = secondConvId
        )
        repo.insertMessage(
            userId = "local",
            role = "model",
            text = "العاصمة هي الرباط.",
            createdAtMillis = laterBase + 1000L,
            conversationId = secondConvId
        )

        db.close()

        val reopenedDb = Room.databaseBuilder(context, SanaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        db = reopenedDb
        val reopenedRepo = LocalChatRepository(
            reopenedDb.chatMessageDao(),
            reopenedDb.chatConversationDao()
        )

        val conversations = reopenedRepo.observeConversations("local").first()
        assertEquals(2, conversations.size)
        assertEquals(secondConvId, conversations[0].conversationId)
        assertEquals(savedId, conversations[1].conversationId)
        assertEquals("خطة تعلم الكوتلن", conversations[1].title)
        assertEquals(2, conversations[1].messageCount)

        val firstMessages = reopenedRepo.observeMessages("local", savedId).first()
        assertEquals(2, firstMessages.size)
        assertEquals("اشرح لي برمجة Kotlin", firstMessages[0].text)

        val secondMessages = reopenedRepo.observeMessages("local", secondConvId).first()
        assertEquals(2, secondMessages.size)
        assertEquals("العاصمة هي الرباط.", secondMessages[1].text)
    }

    @Test
    fun migration1To2PreservesExistingMessagesAndPopulatesConversationHistory() = runBlocking {
        val migrationFile = File(context.filesDir, "test_migration_v1_v2.db")
        if (migrationFile.exists()) migrationFile.delete()

        val rawDb = SQLiteDatabase.openOrCreateDatabase(migrationFile, null)
        rawDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT,
                userId TEXT NOT NULL,
                role TEXT NOT NULL,
                text TEXT NOT NULL,
                sourceTitle TEXT,
                mode TEXT,
                createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        rawDb.execSQL(
            """
            INSERT INTO chat_messages (userId, role, text, createdAtMillis)
            VALUES ('local', 'user', 'رسالة قديمة قبل التحديث', 1500)
            """.trimIndent()
        )
        rawDb.version = 1
        rawDb.close()

        val migratedDb = Room.databaseBuilder(context, SanaDatabase::class.java, migrationFile.absolutePath)
            .addMigrations(SanaDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = LocalChatRepository(
                migratedDb.chatMessageDao(),
                migratedDb.chatConversationDao()
            )
            val messages = migratedRepo.observeMessages("local").first()
            assertEquals(1, messages.size)
            assertEquals("رسالة قديمة قبل التحديث", messages[0].text)

            val conversations = migratedRepo.observeConversations("local").first()
            assertEquals(1, conversations.size)
            assertEquals(DEFAULT_CONVERSATION_ID, conversations[0].conversationId)
            assertEquals("رسالة قديمة قبل التحديث", conversations[0].previewText)
        } finally {
            migratedDb.close()
            migrationFile.delete()
        }
    }

    @Test
    fun uiSavesStartsNewAndRetrievesPreviousConversationFromRoom() {
        runBlocking {
            repo.insertMessage(
                userId = "local",
                role = "user",
                text = "ما فوائد القراءة؟",
                createdAtMillis = 1000L,
                conversationId = DEFAULT_CONVERSATION_ID
            )
            repo.insertMessage(
                userId = "local",
                role = "model",
                text = "القراءة تنمّي الفكر وتوسّع المدارك.",
                createdAtMillis = 2000L,
                conversationId = DEFAULT_CONVERSATION_ID
            )
        }

        val viewModel = FirebaseChatViewModel(repo)
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("ما فوائد القراءة؟").fetchSemanticsNodes().isNotEmpty()
        }

        // Open History sheet -> Save current conversation with custom title
        composeRule.onNodeWithTag("chat_history_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("conversation_history_sheet").assertIsDisplayed()

        composeRule.onNodeWithTag("save_current_conversation_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_conversation_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation_title_input").performTextReplacement("مراجعة القراءة")
        composeRule.onNodeWithTag("confirm_save_conversation_button").performClick()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        composeRule.waitForIdle()

        // Start a brand new conversation -> previous messages disappear from active view
        composeRule.onNodeWithTag("chat_history_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("new_conversation_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("ما فوائد القراءة؟").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithText("ما فوائد القراءة؟").assertCountEquals(0)

        // Open History sheet and retrieve the saved conversation
        composeRule.onNodeWithTag("chat_history_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مراجعة القراءة").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("ما فوائد القراءة؟").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("القراءة تنمّي الفكر وتوسّع المدارك.").assertIsDisplayed()

        // Delete the saved conversation via History sheet
        val savedConvId = runBlocking { repo.observeConversations("local").first().first().conversationId }
        composeRule.onNodeWithTag("chat_history_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("delete_conversation_$savedConvId").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag("conversation_history_empty").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(runBlocking { repo.observeConversations("local").first().isEmpty() })
    }
}
