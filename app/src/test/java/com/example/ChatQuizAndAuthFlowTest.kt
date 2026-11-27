package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ChatContextQuiz
import com.example.data.model.ChatQuizUiState
import com.example.data.model.QuizQuestion
import com.example.data.model.UiState
import com.example.data.repository.parseChatQuizJson
import com.example.firebase.findActivity
import com.example.firebase.resolveWebClientId
import com.example.ui.components.ChatQuizSheetBody
import com.example.ui.screens.ChatDisplayMessage
import com.example.ui.screens.ChatScreenContent
import com.example.ui.theme.SanaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatQuizAndAuthFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun findActivity_unwrapsNestedContextWrappers_andReturnsNullForAppContext() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        assertNull(appContext.findActivity())

        val activity = Robolectric.buildActivity(MainActivity::class.java).get()
        val wrapped = ContextWrapper(ContextWrapper(activity))
        assertEquals(activity, wrapped.findActivity())
    }

    @Test
    fun resolveWebClientId_returnsNonBlankGoogleOAuthClientId() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val clientId = resolveWebClientId(appContext)
        assertTrue(clientId.endsWith(".apps.googleusercontent.com"))
    }

    @Test
    fun parseChatQuizJson_parsesQuestionsOptionsAndExplanation() {
        val raw = """
            {
              "topicTitle": "أساسيات الفيزياء الفلكية",
              "topicSummary": "اختبار سريع حول الثقوب السوداء وسرعة الضوء.",
              "questions": [
                {
                  "question": "كم تستغرق رحلة ضوء الشمس للوصول إلى الأرض تقريبًا؟",
                  "options": ["دقيقة واحدة", "٨ دقائق", "ساعة واحدة", "يوم كامل"],
                  "correctIndex": 1,
                  "explanation": "يقطع الضوء المسافة في نحو ثماني دقائق وعشرين ثانية."
                },
                {
                  "question": "ما الذي يميّز أفق الحدث للثقب الأسود؟",
                  "options": ["نقطة اللاعودة للضوء", "سطح جليدي", "غلاف غازي كثيف", "حلقة كويكبات"],
                  "correctIndex": 0,
                  "explanation": "بعد أفق الحدث لا تستطيع أي إشارة أو ضوء الهروب."
                }
              ]
            }
        """.trimIndent()

        val quiz = parseChatQuizJson(raw)
        assertEquals("أساسيات الفيزياء الفلكية", quiz.topicTitle)
        assertEquals(2, quiz.questions.size)
        assertEquals(1, quiz.questions[0].correctIndex)
        assertEquals(4, quiz.questions[0].options.size)
        assertTrue(quiz.questions[1].explanation.contains("أفق الحدث"))
    }

    @Test(expected = IllegalStateException::class)
    fun parseChatQuizJson_throwsWhenQuestionsEmpty() {
        parseChatQuizJson("""{"topicTitle":"فارغ","questions":[]}""")
    }

    @Test
    fun chatScreen_showsQuizTriggersWhenMessagesExist_andInvokesStartCallback() {
        var quizStartedCount = 0
        val sampleMessages = listOf(
            ChatDisplayMessage(id = "1", isUser = true, text = "اشرحي لي التمثيل الضوئي"),
            ChatDisplayMessage(id = "2", isUser = false, text = "التمثيل الضوئي عملية تحوّل فيها النباتات الضوء إلى طاقة.")
        )

        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(sampleMessages),
                    streamingReply = null,
                    streamingSource = null,
                    isSending = false,
                    chatError = null,
                    selectedMode = null,
                    onSelectMode = null,
                    onSendMessage = {},
                    onRetryLast = {},
                    onDismissError = {},
                    onClearChat = {},
                    exportTextProvider = { "" },
                    onStartChatQuiz = { quizStartedCount++ }
                )
            }
        }

        composeRule.onNodeWithTag("chat_quiz_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_quick_quiz_chip").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("chat_quiz_button").performClick()

        assertEquals(2, quizStartedCount)
    }

    @Test
    fun chatScreen_hidesQuizTriggersWhenConversationIsEmpty() {
        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(emptyList()),
                    streamingReply = null,
                    streamingSource = null,
                    isSending = false,
                    chatError = null,
                    selectedMode = null,
                    onSelectMode = null,
                    onSendMessage = {},
                    onRetryLast = {},
                    onDismissError = {},
                    onClearChat = {},
                    exportTextProvider = { "" },
                    onStartChatQuiz = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_quiz_button").assertDoesNotExist()
        composeRule.onNodeWithTag("chat_quick_quiz_chip").assertDoesNotExist()
    }

    @Test
    fun chatQuizSheetBody_interactiveQuizFlow_updatesScoreExplanationAndSummary() {
        val sampleQuiz = ChatContextQuiz(
            topicTitle = "مراجعة تاريخ الإنترنت",
            topicSummary = "أسئلة تفاعلية من محادثتك حول نشأة الشبكة العنكبوتية.",
            questions = listOf(
                QuizQuestion(
                    question = "من اخترع الشبكة العنكبوتية العالمية (الويب)؟",
                    options = listOf("تيم بيرنرز لي", "آلان تورينغ", "بيل غيتس", "لينوس تورفالدس"),
                    correctIndex = 0,
                    explanation = "طوّر تيم بيرنرز لي الويب في مختبر سيرن عام ١٩٨٩."
                ),
                QuizQuestion(
                    question = "في أي عام ظهر محرك البحث جوجل؟",
                    options = listOf("١٩٨٥", "١٩٩٨", "٢٠٠٨", "٢٠١٢"),
                    correctIndex = 1,
                    explanation = "تأسست شركة جوجل وأُطلق محركها عام ١٩٩٨."
                )
            )
        )

        var state by mutableStateOf<ChatQuizUiState>(ChatQuizUiState.Ready(sampleQuiz))
        var regenerateTriggered = false
        var askedFollowUpQuestion: String? = null

        composeRule.setContent {
            SanaTheme {
                ChatQuizSheetBody(
                    quizState = state,
                    onSelectOption = { idx ->
                        val cur = state as ChatQuizUiState.Ready
                        state = cur.copy(selectedOptions = cur.selectedOptions + (cur.currentIndex to idx))
                    },
                    onNextQuestion = {
                        val cur = state as ChatQuizUiState.Ready
                        state = if (cur.currentIndex < cur.quiz.questions.lastIndex) {
                            cur.copy(currentIndex = cur.currentIndex + 1)
                        } else {
                            cur.copy(showSummary = true)
                        }
                    },
                    onRestartQuiz = {
                        val cur = state as ChatQuizUiState.Ready
                        state = cur.copy(currentIndex = 0, selectedOptions = emptyMap(), showSummary = false)
                    },
                    onRegenerateQuiz = { regenerateTriggered = true },
                    onDismiss = {},
                    onAskAboutQuestion = { q, _ -> askedFollowUpQuestion = q.question }
                )
            }
        }

        // Question 1: select correct option 0
        composeRule.onNodeWithTag("chat_quiz_question_text")
            .assertTextContains("من اخترع الشبكة العنكبوتية العالمية (الويب)؟")
        composeRule.onNodeWithTag("chat_quiz_option_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_explanation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("chat_quiz_score_badge").assertTextContains("1", substring = true)

        // Advance to Question 2
        composeRule.onNodeWithTag("chat_quiz_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_question_text")
            .assertTextContains("في أي عام ظهر محرك البحث جوجل؟")

        // Select wrong option 0 (correct is 1), finish quiz
        composeRule.onNodeWithTag("chat_quiz_option_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_explanation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("chat_quiz_next_button").performClick()
        composeRule.waitForIdle()

        // Summary screen shows 1 / 2 score and supports restart & regenerate
        composeRule.onNodeWithTag("chat_quiz_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_quiz_final_score").assertTextContains("1 / 2", substring = true)

        composeRule.onNodeWithTag("chat_quiz_restart_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_question_text")
            .assertTextContains("من اخترع الشبكة العنكبوتية العالمية (الويب)؟")

        composeRule.onNodeWithTag("chat_quiz_option_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_ask_sana_question_button").performScrollTo().performClick()
        assertNotNull(askedFollowUpQuestion)

        // Verify generating & error states render cleanly
        state = ChatQuizUiState.Generating(streamingPreview = "جارٍ تحليل موضوع الفيزياء…")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_loading").assertIsDisplayed()
        composeRule.onNodeWithText("جارٍ تحليل موضوع الفيزياء…").assertIsDisplayed()

        state = ChatQuizUiState.Error("تعذر توليد الاختبار حاليًا.")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_quiz_error_text").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_quiz_retry_generate_button").performClick()
        assertTrue(regenerateTriggered)
    }
}
