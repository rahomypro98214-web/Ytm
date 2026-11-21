package com.example

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SanaDatabase
import com.example.data.repository.LocalChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.theme.SanaTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatBubbleCopyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: SanaDatabase
    private lateinit var viewModel: FirebaseChatViewModel
    private var userLocalId: Long = 0L
    private var aiLocalId: Long = 0L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = LocalChatRepository(db.chatMessageDao())
        runBlocking {
            userLocalId = repo.insertMessage(
                userId = "local",
                role = "user",
                text = "رسالة المستخدم للنسخ",
                createdAtMillis = 1000L
            )
            aiLocalId = repo.insertMessage(
                userId = "local",
                role = "model",
                text = "رد سنا للنسخ",
                createdAtMillis = 2000L
            )
        }
        viewModel = FirebaseChatViewModel(repo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun copyIconOnUserBubbleCopiesTextToClipboard() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("copy_message_local_$userLocalId")
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithTag("copy_message_local_$userLocalId").performClick()
        composeRule.waitForIdle()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("رسالة المستخدم للنسخ", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun longPressContextMenuOnAiBubbleCopiesTextToClipboard() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("chat_ai_bubble").fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithTag("chat_ai_bubble").performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("copy_message_menu_item_local_$aiLocalId")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("رد سنا للنسخ", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }
}
