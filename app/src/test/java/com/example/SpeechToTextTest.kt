package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SanaDatabase
import com.example.data.model.UiState
import com.example.data.repository.LocalChatRepository
import com.example.ui.components.SpeechToTextController
import com.example.ui.components.mergeDictatedText
import com.example.ui.screens.ChatDisplayMessage
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatScreenContent
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.theme.SanaTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpeechToTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: SanaDatabase
    private lateinit var viewModel: FirebaseChatViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = FirebaseChatViewModel(LocalChatRepository(db.chatMessageDao()))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mergeDictatedTextHandlesEmptyAndAppendedInputs() {
        assertEquals("مرحبا سنا", mergeDictatedText("", "  مرحبا سنا  "))
        assertEquals("أريد تلخيص هذا النص", mergeDictatedText("أريد تلخيص", "هذا النص"))
        assertEquals("نص حالي", mergeDictatedText("نص حالي", "   "))
    }

    @Test
    fun micButtonIsDisplayedAndHandlesUnavailableRecognizerGracefully() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_send_button").assertIsDisplayed()

        composeRule.onNodeWithTag("chat_mic_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("speech_error_banner").assertIsDisplayed()
    }

    @Test
    fun micToggleShowsListeningBannerAndStopButtonAndAllowsSendingSpokenQuery() {
        var isListening by mutableStateOf(false)
        var speechError by mutableStateOf<String?>(null)
        var audioLevel by mutableFloatStateOf(0f)
        var stopCalled = false
        val sentMessages = mutableListOf<String>()

        val fakeSpeechController = SpeechToTextController(
            isListeningProvider = { isListening },
            errorMessageProvider = { speechError },
            audioLevelProvider = { audioLevel },
            onToggleAction = {
                isListening = !isListening
                audioLevel = if (isListening) 0.6f else 0f
                speechError = null
            },
            onStopAction = {
                stopCalled = true
                isListening = false
                audioLevel = 0f
            },
            onDismissErrorAction = { speechError = null }
        )

        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(emptyList<ChatDisplayMessage>()),
                    streamingReply = null,
                    streamingSource = null,
                    isSending = false,
                    chatError = null,
                    selectedMode = null,
                    onSelectMode = null,
                    onSendMessage = { sentMessages.add(it) },
                    onRetryLast = {},
                    onDismissError = {},
                    onClearChat = {},
                    exportTextProvider = { "" },
                    speechController = fakeSpeechController
                )
            }
        }

        composeRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_send_button").assertIsDisplayed()

        composeRule.onNodeWithTag("chat_mic_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("speech_listening_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("speech_stop_button").assertIsDisplayed()

        composeRule.onNodeWithTag("speech_stop_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("speech_listening_banner").assertDoesNotExist()
        assertTrue(stopCalled)

        composeRule.onNodeWithTag("chat_input_field").performTextInput("اقترحي خطة للقراءة اليومية")
        composeRule.onNodeWithTag("chat_send_button").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("اقترحي خطة للقراءة اليومية"), sentMessages)
    }
}
