package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
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
import com.example.ui.screens.ChatDisplayMessage
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.screens.formatShareSnippet
import com.example.ui.theme.SanaTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatBubbleShareTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var db: SanaDatabase
    private lateinit var viewModel: FirebaseChatViewModel
    private var userLocalId: Long = 0L
    private var aiLocalId: Long = 0L

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<Context>() as Application
        db = Room.inMemoryDatabaseBuilder(app, SanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = LocalChatRepository(db.chatMessageDao())
        runBlocking {
            userLocalId = repo.insertMessage(
                userId = "local",
                role = "user",
                text = "ما هي عاصمة المغرب؟",
                createdAtMillis = 1000L
            )
            aiLocalId = repo.insertMessage(
                userId = "local",
                role = "model",
                text = "عاصمة المغرب هي الرباط.",
                sourceTitle = "الأطلس الجغرافي",
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
    fun formatShareSnippetIncludesSpeakerBodyAndOptionalSource() {
        val userSnippet = formatShareSnippet(
            ChatDisplayMessage(id = "u1", isUser = true, text = "  سؤال مهم  ")
        )
        assertEquals("أنت:\nسؤال مهم", userSnippet)

        val aiSnippet = formatShareSnippet(
            ChatDisplayMessage(
                id = "a1",
                isUser = false,
                text = "إجابة مختصرة",
                sourceTitle = "دليل سنا"
            )
        )
        assertEquals("سنا:\nإجابة مختصرة\nالمصدر: دليل سنا", aiSnippet)
    }

    @Test
    fun shareIconOnUserBubbleLaunchesAndroidShareChooser() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("share_message_local_$userLocalId")
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithTag("share_message_local_$userLocalId")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        val innerIntent = startedIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(innerIntent)
        assertEquals(Intent.ACTION_SEND, innerIntent!!.action)
        assertEquals("text/plain", innerIntent.type)
        assertEquals("أنت:\nما هي عاصمة المغرب؟", innerIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun longPressContextMenuShareActionSharesAiSnippetWithSource() {
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

        composeRule.onNodeWithTag("share_message_menu_item_local_$aiLocalId")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        val innerIntent = startedIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(innerIntent)
        assertEquals(Intent.ACTION_SEND, innerIntent!!.action)
        assertEquals("text/plain", innerIntent.type)
        assertEquals(
            "سنا:\nعاصمة المغرب هي الرباط.\nالمصدر: الأطلس الجغرافي",
            innerIntent.getStringExtra(Intent.EXTRA_TEXT)
        )
    }
}
