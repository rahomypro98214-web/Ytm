package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SanaDatabase
import com.example.data.repository.LocalChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.theme.SanaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatScreenClearMenuTest {

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
            repo.insertMessage(userId = "local", role = "user", text = "مرحبا", createdAtMillis = 1000L)
            repo.insertMessage(userId = "local", role = "model", text = "أهلاً بك!", createdAtMillis = 2000L)
        }
        viewModel = FirebaseChatViewModel(repo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun topBarMenuClearsChatFromUiAndRoomDatabase() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("chat_user_bubble").fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("chat_user_bubble").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_ai_bubble").assertIsDisplayed()

        composeRule.onNodeWithTag("chat_options_menu_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("clear_chat_menu_item").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("confirm_clear_chat_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("chat_user_bubble").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onAllNodesWithTag("chat_user_bubble").assertCountEquals(0)
        composeRule.onAllNodesWithTag("chat_ai_bubble").assertCountEquals(0)

        runBlocking {
            val remaining = repo.observeMessages("local").first()
            assertTrue(remaining.isEmpty())
        }
    }
}
