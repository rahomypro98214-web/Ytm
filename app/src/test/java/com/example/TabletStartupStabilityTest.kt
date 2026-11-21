package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DEFAULT_CONVERSATION_ID
import com.example.data.local.SanaDatabase
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.SanaTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp")
class TabletStartupStabilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivityLaunchesCleanlyOnTabletAndCompletesOnboardingWithoutCrashing() {
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeRule.waitForIdle()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1280dp")
class PortraitTabletAuthAndMigrationTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun authScreenRendersCleanlyOnPortraitTablet() {
        composeRule.setContent {
            SanaTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun databaseMigration1To2HandlesLegacyV1IndicesWithoutCrashing() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_legacy_migration_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        try {
            // Create a v1 database that includes an older index on chat_messages
            val helper = FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS chat_messages (
                                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    remoteId TEXT,
                                    userId TEXT NOT NULL,
                                    role TEXT NOT NULL,
                                    text TEXT NOT NULL,
                                    sourceTitle TEXT,
                                    mode TEXT,
                                    createdAtMillis INTEGER NOT NULL
                                )
                                """.trimIndent()
                            )
                            db.execSQL(
                                "CREATE INDEX IF NOT EXISTS index_chat_messages_userId " +
                                    "ON chat_messages (userId)"
                            )
                            db.execSQL(
                                "INSERT INTO chat_messages (remoteId, userId, role, text, createdAtMillis) " +
                                    "VALUES (NULL, 'user1', 'user', 'رسالة قديمة', 1000)"
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    })
                    .build()
            )
            helper.writableDatabase.close()
            helper.close()

            val migratedDb = Room.databaseBuilder(context, SanaDatabase::class.java, dbName)
                .addMigrations(SanaDatabase.MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .allowMainThreadQueries()
                .build()

            try {
                val messages = migratedDb.chatMessageDao()
                    .getMessagesForConversationOnce("user1", DEFAULT_CONVERSATION_ID)
                assertEquals(1, messages.size)
                assertEquals("رسالة قديمة", messages.first().text)
            } finally {
                migratedDb.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun corruptedPreferencesDataStoreRecoversWithoutCrashing() = runTest {
        val tempFile = File.createTempFile("corrupted_prefs_", ".preferences_pb")
        try {
            tempFile.writeBytes(byteArrayOf(0x7F, 0x11, 0x22, 0x33, 0x44))
            val dataStore = PreferenceDataStoreFactory.create(
                corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
                    androidx.datastore.preferences.core.emptyPreferences()
                },
                scope = backgroundScope,
                produceFile = { tempFile }
            )
            val repo = UserPreferencesRepository(dataStore)
            assertFalse(repo.hasCompletedOnboarding.first())
        } finally {
            tempFile.delete()
        }
    }
}
