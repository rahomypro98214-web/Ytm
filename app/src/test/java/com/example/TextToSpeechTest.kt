package com.example

import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.ChatMode
import com.example.data.model.UiState
import com.example.ui.components.TextToSpeechController
import com.example.ui.components.splitTextForTts
import com.example.ui.components.stripMarkdownForSpeech
import com.example.ui.screens.ChatDisplayMessage
import com.example.ui.screens.ChatScreenContent
import com.example.ui.theme.SanaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TextToSpeechTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stripMarkdownForSpeechRemovesFormattingMarkersCleanly() {
        val raw = """
            # عنوان رئيسي
            - هذا **نص عريض** و`كود`
            اقرأ [المقال](https://example.com) للمزيد.
        """.trimIndent()

        val cleaned = stripMarkdownForSpeech(raw)
        assertEquals("عنوان رئيسي هذا نص عريض وكود اقرأ المقال للمزيد.", cleaned)
    }

    @Test
    fun splitTextForTtsSplitsOnWordBoundaries() {
        val longText = List(12) { "كلمة$it" }.joinToString(" ")
        val chunks = splitTextForTts(longText, maxChunkChars = 25)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 25 })
        assertEquals(longText, chunks.joinToString(" "))
    }

    @Test
    fun aiBubbleShowsTtsButtonAndTogglesSpeakingState() {
        val messages = listOf(
            ChatDisplayMessage(id = "u1", isUser = true, text = "مرحبا"),
            ChatDisplayMessage(id = "a1", isUser = false, text = "**أهلاً بك!** كيف أساعدك اليوم؟")
        )

        var activeId by mutableStateOf<String?>(null)
        var spokenText = ""
        val controller = TextToSpeechController(
            speakingMessageIdProvider = { activeId },
            errorMessageProvider = { null },
            onToggleSpeakAction = { id, text ->
                if (activeId == id) {
                    activeId = null
                } else {
                    activeId = id
                    spokenText = stripMarkdownForSpeech(text)
                }
            },
            onStopAction = { activeId = null },
            onDismissErrorAction = {}
        )

        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(messages),
                    streamingReply = null,
                    streamingSource = null,
                    isSending = false,
                    chatError = null,
                    selectedMode = ChatMode.CHAT,
                    onSelectMode = {},
                    onSendMessage = {},
                    onRetryLast = {},
                    onDismissError = {},
                    onClearChat = {},
                    exportTextProvider = { "" },
                    ttsController = controller
                )
            }
        }

        // Only AI bubble has TTS listen button
        composeRule.onAllNodesWithTag("tts_button_u1").assertCountEquals(0)
        composeRule.onNodeWithTag("tts_button_a1").assertIsDisplayed()

        // Tap Listen -> active speaking badge appears
        composeRule.onNodeWithTag("tts_button_a1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tts_speaking_badge_a1").assertIsDisplayed()
        assertEquals("أهلاً بك! كيف أساعدك اليوم؟", spokenText)

        // Tap Stop -> active speaking badge disappears
        composeRule.onNodeWithTag("tts_button_a1").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("tts_speaking_badge_a1").assertCountEquals(0)
    }

    @Test
    fun defaultAndroidTtsControllerSpeaksCleanedResponseText() {
        val messages = listOf(
            ChatDisplayMessage(id = "a1", isUser = false, text = "**مرحبا!** أنا سنا.")
        )

        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(messages),
                    streamingReply = null,
                    streamingSource = null,
                    isSending = false,
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

        composeRule.waitForIdle()
        val lastTts = ShadowTextToSpeech.getLastTextToSpeechInstance()
        assertNotNull(lastTts)
        val shadowTts = Shadows.shadowOf(lastTts!!)
        shadowTts.onInitListener?.onInit(TextToSpeech.SUCCESS)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tts_button_a1").performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()

        assertEquals("مرحبا! أنا سنا.", shadowTts.lastSpokenText)
    }
}
