package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SanaDatabase
import com.example.data.repository.LocalChatRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.SettingsMenuDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FirebaseChatViewModel
import com.example.ui.theme.SanaTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var db: SanaDatabase
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var viewModel: FirebaseChatViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SanaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val uiPrefsFile = File(tempFolder.newFolder("datastore"), "ui_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { uiPrefsFile }
        )
        prefsRepo = UserPreferencesRepository(dataStore)
        viewModel = FirebaseChatViewModel(
            localChatRepo = LocalChatRepository(db.chatMessageDao()),
            userPreferencesRepo = prefsRepo
        )
    }

    @After
    fun tearDown() {
        db.close()
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun dataStorePersistsAndClearsPreferredNameAcrossRepositoryInstances() = runBlocking {
        val reloadFile = File(tempFolder.newFolder("reload_datastore"), "reload_prefs.preferences_pb")

        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstRepo = UserPreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = firstScope,
                produceFile = { reloadFile }
            )
        )
        assertNull(firstRepo.preferredName.first())
        firstRepo.setPreferredName("   أبو سارة   ")
        assertEquals("أبو سارة", firstRepo.preferredName.first())
        firstScope.coroutineContext[Job]!!.cancelAndJoin()

        // Reopen from the exact same preferences_pb file to verify disk persistence
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reloadedRepo = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(
                    scope = secondScope,
                    produceFile = { reloadFile }
                )
            )
            assertEquals("أبو سارة", reloadedRepo.preferredName.first())

            reloadedRepo.clearPreferredName()
            assertNull(reloadedRepo.preferredName.first())
        } finally {
            secondScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun settingsMenuSavesPreferredNameToDataStoreAndUpdatesPersonalizedGreeting() {
        composeRule.setContent {
            SanaTheme {
                ChatScreen(chatViewModel = viewModel)
            }
        }

        // Default greeting before nickname is set
        composeRule.onNodeWithTag("chat_welcome_greeting")
            .assertIsDisplayed()
            .assertTextEquals("مرحبًا! أنا سنا")

        // Open Chat options menu -> Settings
        composeRule.onNodeWithTag("chat_options_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_settings_menu_item")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_menu_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("preferred_name_input").performTextInput("ليلى")
        composeRule.waitForIdle()

        // Live preview updates inside Settings dialog
        composeRule.onNodeWithTag("settings_greeting_preview")
            .assertTextEquals("مرحبًا، ليلى! أنا سنا")

        // Save -> persisted in DataStore and reflected immediately in welcome greeting
        composeRule.onNodeWithTag("save_preferred_name_button").performClick()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            runBlocking { prefsRepo.preferredName.first() == "ليلى" }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_welcome_greeting")
            .assertTextEquals("مرحبًا، ليلى! أنا سنا")

        // Re-open Settings and reset to default
        composeRule.onNodeWithTag("chat_options_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_settings_menu_item").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("clear_preferred_name_button")
            .assertIsDisplayed()
            .performClick()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            runBlocking { prefsRepo.preferredName.first() == null }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_welcome_greeting")
            .assertTextEquals("مرحبًا! أنا سنا")
    }

    @Test
    fun settingsDialogDisplaysExistingNicknameAndAllowsEditing() {
        var savedName = ""
        composeRule.setContent {
            SanaTheme {
                SettingsMenuDialog(
                    currentPreferredName = "خالد",
                    accountDisplayName = "khaled@example.com",
                    onSavePreferredName = { savedName = it },
                    onClearPreferredName = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_greeting_preview")
            .assertTextEquals("مرحبًا، خالد! أنا سنا")

        composeRule.onNodeWithTag("preferred_name_input")
            .performTextReplacement("د. خالد")
        composeRule.onNodeWithTag("save_preferred_name_button").performClick()
        composeRule.waitForIdle()

        assertEquals("د. خالد", savedName)
    }
}
