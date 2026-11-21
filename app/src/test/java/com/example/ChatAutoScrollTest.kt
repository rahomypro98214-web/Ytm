package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SanaDatabase
import com.example.data.repository.LocalChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.theme.SanaTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatAutoScrollTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: SanaDatabase
    private lateinit var repo: LocalChatRepository
    private lateinit var viewModel: FirebaseChatViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LocalChatRepository(db.chatMessageDao())
        runBlocking {
            for (i in 1..20) {
                repo.insertMessage(
                    userId = "local",
                    role = if (i % 2 == 1) "user" else "model",
                    text = "رسالة سابقة رقم $i",
                    createdAtMillis = i * 1000L
                )
            }
        }
        viewModel = FirebaseChatViewModel(repo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun listAutoScrollsToBottomWhenUserOrAiAddsNewMessage() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("رسالة سابقة رقم 20").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("رسالة سابقة رقم 20").assertIsDisplayed()

        runBlocking {
            repo.insertMessage(
                userId = "local",
                role = "user",
                text = "رسالة مستخدم جديدة في الأسفل",
                createdAtMillis = 21_000L
            )
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("رسالة مستخدم جديدة في الأسفل").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("رسالة مستخدم جديدة في الأسفل").assertIsDisplayed()

        runBlocking {
            repo.insertMessage(
                userId = "local",
                role = "model",
                text = "رد ذكاء اصطناعي جديد في الأسفل",
                createdAtMillis = 22_000L
            )
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("رد ذكاء اصطناعي جديد في الأسفل").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("رد ذكاء اصطناعي جديد في الأسفل").assertIsDisplayed()
    }
}
