package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.SanaTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun onboardingPagerNavigatesForwardBackAndCompletes() {
        var finishedCount = 0
        composeRule.setContent {
            SanaTheme {
                OnboardingScreen(onFinish = { finishedCount++ })
            }
        }

        // Page 0 displays welcome & local Room chat history highlight
        composeRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_page_0").assertIsDisplayed()
        composeRule.onNodeWithText("مرحبًا بك في سنا").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()

        // Advance to Page 1
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_page_1").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_back_button").assertIsDisplayed()

        // Go back to Page 0
        composeRule.onNodeWithTag("onboarding_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_page_0").assertIsDisplayed()

        // Advance to Page 1 -> Page 2
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_page_2").assertIsDisplayed()

        // Finish on last page
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()
        assertTrue(finishedCount == 1)
    }

    @Test
    fun onboardingSkipButtonCompletesImmediately() {
        var skipped = false
        composeRule.setContent {
            SanaTheme {
                OnboardingScreen(onFinish = { skipped = true })
            }
        }

        composeRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeRule.waitForIdle()
        assertTrue(skipped)
    }

    @Test
    fun userPreferencesPersistsFirstLaunchOnboardingFlag() = runBlocking {
        val prefsFile = File(tempFolder.root, "onboarding_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        val repo = UserPreferencesRepository(dataStore)

        assertFalse(repo.hasCompletedOnboarding.first())
        repo.setOnboardingCompleted(true)
        assertTrue(repo.hasCompletedOnboarding.first())
    }
}
