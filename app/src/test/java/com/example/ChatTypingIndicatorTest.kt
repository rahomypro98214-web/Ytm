package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.model.ChatMode
import com.example.data.model.UiState
import com.example.ui.screens.ChatDisplayMessage
import com.example.ui.screens.ChatScreenContent
import com.example.ui.theme.SanaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatTypingIndicatorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun temporaryTypingBubbleAppearsWhileGeneratingAndDisappearsWhenDone() {
        var isSending by mutableStateOf(true)
        var streamingReply by mutableStateOf<String?>(null)
        var messages by mutableStateOf(
            listOf(ChatDisplayMessage(id = "1", isUser = true, text = "اشرح لي الذكاء الاصطناعي"))
        )

        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(messages),
                    streamingReply = streamingReply,
                    streamingSource = null,
                    isSending = isSending,
                    chatError = null,
                    selectedMode = ChatMode.CHAT,
                    onSelectMode = {},
                    onSendMessage = {},
                    onRetryLast = {},
                    onDismissError = {},
                    onClearChat = {},
                    exportTextProvider = { "" }
                )
            }
        }

        // Before first token arrives, temporary typing bubble + animated dots + status text show
        composeRule.onNodeWithTag("chat_streaming_bubble").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_typing_dots").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_typing_status_text").assertIsDisplayed()

        // While streaming partial tokens, bubble + typing dots remain visible and status placeholder hides
        streamingReply = "الذكاء الاصطناعي هو..."
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_streaming_bubble").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_typing_dots").assertIsDisplayed()
        composeRule.onAllNodesWithTag("chat_typing_status_text").assertCountEquals(0)

        // Once response completes, temporary typing bubble disappears and final AI bubble shows
        isSending = false
        streamingReply = null
        messages = messages + ChatDisplayMessage(
            id = "2",
            isUser = false,
            text = "الذكاء الاصطناعي هو محاكاة الذكاء البشري."
        )
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("chat_streaming_bubble").assertCountEquals(0)
        composeRule.onNodeWithTag("chat_ai_bubble").assertIsDisplayed()
    }
}
