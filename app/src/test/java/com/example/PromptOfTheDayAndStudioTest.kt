package com.example

import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.ChatMode
import com.example.data.model.UiState
import com.example.data.repository.StudioAspectRatio
import com.example.data.repository.VideoScenePlan
import com.example.data.repository.VideoStoryboardPlan
import com.example.ui.GeneratedImageResult
import com.example.ui.components.buildDailyPromptsCarousel
import com.example.ui.screens.ChatScreenContent
import com.example.ui.screens.StudioScreenContent
import com.example.ui.theme.SanaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PromptOfTheDayAndStudioTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dailyPromptsRotateByDayOfYearAndMarkFirstFeatured() {
        val day0 = buildDailyPromptsCarousel(dayOfYear = 0)
        val day1 = buildDailyPromptsCarousel(dayOfYear = 1)

        assertTrue(day0.isNotEmpty())
        assertTrue(day0.first().isFeaturedToday)
        assertEquals(1, day0.count { it.isFeaturedToday })
        assertNotEquals(day0.first().id, day1.first().id)
    }

    @Test
    fun promptOfTheDayCarouselDisplaysTriggersSendAndCollapsesCleanly() {
        var sentPrompt = ""
        composeRule.setContent {
            SanaTheme {
                ChatScreenContent(
                    messagesState = UiState.Success(emptyList()),
                    streamingReply = null,
                    streamingSource = null,
                    isSending = false,
                    chatError = null,
                    selectedMode = ChatMode.CHAT,
                    onSelectMode = {},
                    onSendMessage = { sentPrompt = it },
                    onRetryLast = {},
                    onDismissError = {},
                    onClearChat = {},
                    exportTextProvider = { "" }
                )
            }
        }

        composeRule.onNodeWithTag("prompt_of_day_section").assertIsDisplayed()
        composeRule.onNodeWithTag("prompt_of_day_carousel").assertIsDisplayed()
        composeRule.onNodeWithTag("prompt_of_day_card_0").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertTrue(sentPrompt.isNotBlank())

        composeRule.onNodeWithTag("toggle_prompt_carousel_button").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("prompt_of_day_carousel").assertCountEquals(0)
    }

    @Test
    fun studioImageModeTriggersGenerationAndRendersResult() {
        var capturedPrompt = ""
        var capturedRatio: StudioAspectRatio? = null
        val testBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        composeRule.setContent {
            SanaTheme {
                StudioScreenContent(
                    imageState = UiState.Success(
                        GeneratedImageResult(
                            prompt = "مكتبة عربية مضيئة",
                            aspectRatio = StudioAspectRatio.LANDSCAPE,
                            bitmap = testBitmap
                        )
                    ),
                    storyboardState = null,
                    sceneImages = emptyMap(),
                    onGenerateImage = { prompt, ratio, _ ->
                        capturedPrompt = prompt
                        capturedRatio = ratio
                    },
                    onGenerateVideoStoryboard = { _, _ -> },
                    onGenerateSceneFrame = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("studio_image_prompt_input").performTextInput("مكتبة عربية مضيئة")
        composeRule.onNodeWithTag("studio_image_aspect_landscape").performScrollTo().performClick()
        composeRule.onNodeWithTag("studio_generate_image_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("مكتبة عربية مضيئة", capturedPrompt)
        assertEquals(StudioAspectRatio.LANDSCAPE, capturedRatio)
        composeRule.onNodeWithTag("studio_generated_image").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun studioVideoModeDisplaysStoryboardPlayerAndTriggersSceneFrameGeneration() {
        var generatedTopic = ""
        var generatedSceneIndex = -1
        val plan = VideoStoryboardPlan(
            title = "تاريخ الخط العربي",
            mood = "ملهم وهادئ",
            aspectRatio = StudioAspectRatio.LANDSCAPE,
            scenes = listOf(
                VideoScenePlan(
                    sceneNumber = 1,
                    durationSeconds = 5,
                    cameraMotion = "تقريب ناعم",
                    visualPrompt = "قلم قصب وورق مخطوطات عربي قديم",
                    narrationArabic = "بدأ الخط العربي رحلته الفنية منذ قرون طويلة."
                )
            )
        )

        composeRule.setContent {
            SanaTheme {
                StudioScreenContent(
                    imageState = null,
                    storyboardState = UiState.Success(plan),
                    sceneImages = emptyMap(),
                    onGenerateImage = { _, _, _ -> },
                    onGenerateVideoStoryboard = { topic, _ -> generatedTopic = topic },
                    onGenerateSceneFrame = { idx, _, _ -> generatedSceneIndex = idx }
                )
            }
        }

        composeRule.onNodeWithTag("studio_mode_video").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_video_prompt_input").performTextInput("تاريخ الخط العربي")
        composeRule.onNodeWithTag("studio_generate_video_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("تاريخ الخط العربي", generatedTopic)
        composeRule.onNodeWithTag("studio_video_preview_player").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("studio_scene_card_0").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("studio_generate_scene_frame_0").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(0, generatedSceneIndex)
    }
}
